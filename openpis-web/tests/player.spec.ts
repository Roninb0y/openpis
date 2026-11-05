import { expect, test, type Page } from '@playwright/test'
import type { PlayerCurrent, PlayerPlaylistItem } from '../src/api/player'

const image = (id: number, duration = 60): PlayerPlaylistItem => ({
  materialId: id, title: `Image ${id}`, materialType: 'image',
  url: `/media/${id}.svg`, duration
})
const text = (id: number, duration = 60): PlayerPlaylistItem => ({
  materialId: id, title: `Text ${id}`, materialType: 'text',
  url: `/media/${id}.txt`, duration
})

function plan(items: PlayerPlaylistItem[]): PlayerCurrent {
  return {
    planId: 1, planName: 'Daily', programId: 1, programName: 'Main', playType: 'loop',
    layoutWidth: 1920, layoutHeight: 1080,
    modules: [{
      moduleId: 'm1', name: 'Main', type: items[0]?.materialType || 'image',
      x: 0, y: 0, w: 1920, h: 1080, zIndex: 1,
      playlist: { id: 1, name: 'Playlist', type: 'video', items }
    }]
  }
}

type Log = { materialId: number; programId: number; startedAt: string; endedAt: string }

async function openPlayer(page: Page, items: PlayerPlaylistItem[], timed = true) {
  const state = { current: plan(items) as PlayerCurrent | null, logs: [] as Log[], requests: 0 }
  await page.route(/\/api\/(player\/(current|log)|device\/heartbeat)(\?|$)/, async route => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/player/current') {
      await route.fulfill({ json: { code: 200, data: state.current } })
      state.requests++
    } else {
      if (path === '/api/player/log') state.logs.push(...route.request().postDataJSON())
      await route.fulfill({ json: { code: 200, data: null } })
    }
  })
  await page.route('**/media/**', async route => {
    const path = new URL(route.request().url()).pathname
    if (path.endsWith('.txt')) {
      await route.fulfill({ contentType: 'text/plain', body: `Subtitle ${path}` })
    } else {
      await route.fulfill({ contentType: 'image/svg+xml', body:
        `<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080"><rect width="1920" height="1080" fill="#164e63"/><text x="120" y="540" font-size="100" fill="white">${path}</text></svg>` })
    }
  })
  await page.routeWebSocket('**/ws/player?*', () => {})
  if (timed) {
    const now = new Date('2026-11-04T10:00:00')
    await page.clock.install({ time: now })
    await page.clock.pauseAt(now)
  }
  await page.goto('/player?code=TEST-01')
  await expect(page.locator('.module-player')).toBeVisible()
  return state
}

async function poll(page: Page, state: { requests: number }, milliseconds = 30000) {
  const count = state.requests
  const response = page.waitForResponse(r => r.url().includes('/api/player/current'))
  await page.clock.runFor(milliseconds)
  await (await response).finished()
  await expect.poll(() => state.requests).toBeGreaterThan(count)
  // Let the Axios response and Vue's render queue complete before advancing time again.
  await page.evaluate(() => new Promise<void>(resolve => queueMicrotask(resolve)))
}

test('identical polling preserves a 60-second image and logs one completed cycle', async ({ page }) => {
  const state = await openPlayer(page, [image(1), image(2)])
  await expect(page.locator('img')).toHaveAttribute('src', '/media/1.svg')
  await poll(page, state)
  await expect(page.locator('img')).toHaveAttribute('src', '/media/1.svg')
  expect(state.logs).toHaveLength(0)
  await poll(page, state)
  await expect(page.locator('img')).toHaveAttribute('src', '/media/2.svg')
  await expect.poll(() => state.logs.length).toBe(1)
  expect(state.logs[0].materialId).toBe(1)
})

test('identical polling preserves subtitle duration', async ({ page }) => {
  const state = await openPlayer(page, [text(1), text(2)])
  await expect(page.locator('.text-scroll')).toHaveText('Subtitle /media/1.txt')
  await poll(page, state)
  await poll(page, state)
  await expect(page.locator('.text-scroll')).toHaveText('Subtitle /media/2.txt')
})

test('a single image repeats without waiting for polling', async ({ page }) => {
  const state = await openPlayer(page, [image(1, 5)])
  await page.clock.runFor(5000)
  await expect.poll(() => state.logs.length).toBe(1)
  await page.clock.runFor(5000)
  await expect.poll(() => state.logs.length).toBe(2)
  await expect(page.locator('img')).toHaveAttribute('src', '/media/1.svg')
})

test('a shorter playlist restarts from its first item', async ({ page }) => {
  const state = await openPlayer(page, [image(1, 5), image(2, 60)])
  await page.clock.runFor(5000)
  await expect(page.locator('img')).toHaveAttribute('src', '/media/2.svg')
  state.current!.modules[0].playlist!.items = [image(3, 60)]
  await poll(page, state, 25000)
  await expect(page.locator('img')).toHaveAttribute('src', '/media/3.svg')
})

test('geometry-only updates preserve the active timer', async ({ page }) => {
  const state = await openPlayer(page, [image(1), image(2)])
  state.current!.modules[0].w = 960
  await poll(page, state)
  await expect(page.locator('.module-box')).toHaveCSS('width', '640px')
  await poll(page, state)
  await expect(page.locator('img')).toHaveAttribute('src', '/media/2.svg')
})

test('empty playlists stop playback and recover when populated', async ({ page }) => {
  const state = await openPlayer(page, [image(1, 5)])
  state.current!.modules[0].playlist!.items = []
  await poll(page, state)
  await expect(page.getByText('Main（未绑定播表）')).toBeVisible()
  const count = state.logs.length
  await page.clock.runFor(5000)
  expect(state.logs).toHaveLength(count)
  state.current!.modules[0].playlist!.items = [image(2)]
  await poll(page, state, 25000)
  await expect(page.locator('img')).toHaveAttribute('src', '/media/2.svg')
})

test('a single real video plays for at least two complete cycles', async ({ page }) => {
  // Generate an original, short WebM in Chromium; no downloaded fixture or fake ended event.
  await page.goto('/')
  const bytes = await page.evaluate(async () => {
    const canvas = document.createElement('canvas')
    canvas.width = 160
    canvas.height = 90
    const ctx = canvas.getContext('2d')!
    const stream = canvas.captureStream(10)
    const recorder = new MediaRecorder(stream, { mimeType: 'video/webm;codecs=vp8' })
    const chunks: Blob[] = []
    recorder.ondataavailable = event => chunks.push(event.data)
    const recording = new Promise<number[]>(resolve => {
      recorder.onstop = async () => {
        stream.getTracks().forEach(track => track.stop())
        resolve(Array.from(new Uint8Array(await new Blob(chunks).arrayBuffer())))
      }
    })
    recorder.start()
    let frame = 0
    const timer = setInterval(() => {
      ctx.fillStyle = frame++ % 2 ? '#164e63' : '#0e7490'
      ctx.fillRect(0, 0, 160, 90)
    }, 100)
    setTimeout(() => { clearInterval(timer); recorder.stop() }, 800)
    return recording
  })
  const item = { ...image(1), materialType: 'video', url: '/video.webm' }
  await page.route('**/video.webm', route => route.fulfill({
    contentType: 'video/webm', body: Buffer.from(bytes)
  }))
  await page.addInitScript(() => {
    const events = { ended: 0, errors: 0 }
    ;(window as typeof window & { videoEvents: typeof events }).videoEvents = events
    document.addEventListener('ended', event => {
      if (event.target instanceof HTMLVideoElement) events.ended++
    }, true)
    document.addEventListener('error', event => {
      if (event.target instanceof HTMLVideoElement) events.errors++
    }, true)
  })
  const state = await openPlayer(page, [item], false)
  await expect(page.locator('video')).toBeVisible()
  const videoEvents = () => page.evaluate(() =>
    (window as typeof window & { videoEvents: { ended: number; errors: number } }).videoEvents)
  await expect.poll(async () => (await videoEvents()).ended, { timeout: 15000 }).toBeGreaterThanOrEqual(2)
  expect((await videoEvents()).errors).toBe(0)
  await expect.poll(() => state.logs.length, { timeout: 15000 }).toBeGreaterThanOrEqual(2)
  expect(state.logs.every(log => log.materialId === 1)).toBe(true)
})

test('late subtitle responses cannot overwrite a replacement playlist', async ({ page }) => {
  const state = await openPlayer(page, [image(1)])
  let release!: () => void
  const gate = new Promise<void>(resolve => { release = resolve })
  let requested = false
  await page.route('**/media/2.txt', async route => {
    requested = true
    await gate
    await route.fulfill({ contentType: 'text/plain', body: 'Stale subtitle' })
  })
  state.current!.modules[0].playlist!.items = [text(2)]
  await poll(page, state)
  await expect.poll(() => requested).toBe(true)
  state.current!.modules[0].playlist!.items = [text(3, 10), text(4, 10)]
  await poll(page, state)
  await expect(page.locator('.text-scroll')).toHaveText('Subtitle /media/3.txt')
  const oldResponse = page.waitForResponse('**/media/2.txt')
  release()
  await (await oldResponse).finished()
  await page.clock.runFor(1000)
  await expect(page.locator('.text-scroll')).toHaveText('Subtitle /media/3.txt')
  await page.clock.runFor(9000)
  await expect(page.locator('.text-scroll')).toHaveText('Subtitle /media/4.txt')
})

test('a broken single image retries without a tight request loop', async ({ page }) => {
  const state = await openPlayer(page, [image(1)])
  let requests = 0
  await page.route('**/media/broken.svg', async route => {
    requests++
    await route.fulfill({ status: 404, body: 'missing' })
  })
  state.current!.modules[0].playlist!.items = [{ ...image(2), url: '/media/broken.svg' }]
  await poll(page, state)
  await expect.poll(() => requests).toBe(1)
  await page.clock.runFor(500)
  expect(requests).toBe(1)
  await page.clock.runFor(500)
  await expect.poll(() => requests).toBe(2)
})

test('player fits desktop and portrait screens', async ({ page }, testInfo) => {
  await openPlayer(page, [image(1)])
  for (const viewport of [{ width: 1280, height: 720 }, { width: 390, height: 844 }]) {
    await page.setViewportSize(viewport)
    await expect(page.locator('.stage')).toHaveCSS('width', `${viewport.width}px`)
    const box = await page.locator('.stage').boundingBox()
    expect(box!.x).toBeGreaterThanOrEqual(0)
    expect(box!.y).toBeGreaterThanOrEqual(0)
    expect(box!.width).toBeLessThanOrEqual(viewport.width)
    expect(box!.height).toBeLessThanOrEqual(viewport.height)
    await page.screenshot({ path: testInfo.outputPath(`player-${viewport.width}.png`) })
  }
})
