<script setup lang="ts">
/**
 * PdfViewer — self-contained PDF renderer using pdfjs-dist.
 *
 * Key design decisions:
 *  • Worker is bundled locally via Vite's ?url import — NO CDN / unpkg.
 *  • CMaps are NOT configured. Standard western PDFs render fine without
 *    them.  If you later need CJK support, set `cMapUrl` to a locally
 *    served copy of `node_modules/pdfjs-dist/cmaps/` and `cMapPacked: true`.
 *  • Every load cycle follows a strict cleanup → load → render sequence
 *    to avoid the white-screen bug that occurs when a previous render task
 *    races against a destroyed pdf document.
 */
import { ref, watch, onUnmounted, onMounted, nextTick, shallowRef } from 'vue'
import * as pdfjsLib from 'pdfjs-dist'
import pdfjsWorkerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import { useHotkeys } from '@/composables/useHotkeys'
import {
  ChevronLeft, ChevronRight, ZoomIn, ZoomOut,
  Moon, AlertCircle, Loader2
} from 'lucide-vue-next'

// ── Worker — served from the local bundle, never from unpkg ──────────────
pdfjsLib.GlobalWorkerOptions.workerSrc = pdfjsWorkerUrl

// ── Props ────────────────────────────────────────────────────────────────
const props = defineProps<{
  blobUrl: string
}>()

// ── State ────────────────────────────────────────────────────────────────
const pdfDoc = shallowRef<any>(null)
const renderTask = shallowRef<any>(null)

const pageNum = ref(1)
const pageCount = ref(0)
const scale = ref(1.3)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const isRendering = ref(false)
const isLoading = ref(false)
const error = ref<string | null>(null)
const invertColors = ref(false)
const mounted = ref(false)

onMounted(() => { mounted.value = true })

// ── Helpers ──────────────────────────────────────────────────────────────

/** Cancel any in-flight canvas render (safe to call repeatedly). */
const cancelRender = async () => {
  if (renderTask.value) {
    try { await renderTask.value.cancel() } catch { /* already done */ }
    renderTask.value = null
  }
}

/** Zero-out the canvas so no stale frame is visible. */
const clearCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height)
  canvas.width = 0
  canvas.height = 0
}

/** Full teardown: cancel render → destroy doc → clear canvas → reset state. */
const cleanup = async () => {
  await cancelRender()
  if (pdfDoc.value) {
    pdfDoc.value.destroy()
    pdfDoc.value = null
  }
  clearCanvas()
  pageNum.value = 1
  pageCount.value = 0
}

// ── Rendering ────────────────────────────────────────────────────────────

const renderPage = async (num: number) => {
  if (!canvasRef.value || !pdfDoc.value) return

  await cancelRender()
  isRendering.value = true
  error.value = null

  try {
    const page = await pdfDoc.value.getPage(num)
    const canvas = canvasRef.value
    if (!canvas) return              // component unmounted during await

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const dpr = window.devicePixelRatio || 1
    const viewport = page.getViewport({ scale: scale.value })

    canvas.width  = Math.floor(viewport.width  * dpr)
    canvas.height = Math.floor(viewport.height * dpr)
    canvas.style.width  = `${Math.floor(viewport.width)}px`
    canvas.style.height = `${Math.floor(viewport.height)}px`

    const task = page.render({
      canvasContext: ctx,
      viewport,
      transform: dpr !== 1 ? [dpr, 0, 0, dpr, 0, 0] : undefined,
    })
    renderTask.value = task
    await task.promise
  } catch (err: any) {
    if (err?.name !== 'RenderingCancelledException') {
      console.error('PDF render error:', err)
      if (err?.name !== 'UnknownErrorException') {
        error.value = `Render error: ${err.message}`
      }
    }
  } finally {
    isRendering.value = false
  }
}

const loadPdf = async (url: string) => {
  await cleanup()
  isLoading.value = true
  error.value = null

  try {
    const pdf = await pdfjsLib.getDocument({
      url,
      verbosity: 0,              // suppress non-fatal font warnings
    }).promise

    pdfDoc.value = pdf
    pageCount.value = pdf.numPages
    pageNum.value = 1

    await nextTick()              // ensure canvas is in the DOM
    if (canvasRef.value) {
      await renderPage(1)
    }
  } catch (e: any) {
    console.error('Failed to load PDF:', e)
    error.value = `Could not load PDF: ${e.message}`
  } finally {
    isLoading.value = false
  }
}

// ── Watchers ─────────────────────────────────────────────────────────────

// flush:'post' guarantees the canvas element is in the DOM before we touch it.
// This is the core fix for the white-screen-on-second-invocation bug.
watch(() => props.blobUrl, (url) => {
  if (url) loadPdf(url)
}, { immediate: true, flush: 'post' })

// Re-render when the user zooms or changes pages.
watch([scale, pageNum], () => {
  if (pdfDoc.value) renderPage(pageNum.value)
})

// ── Page / Zoom controls ─────────────────────────────────────────────────
const nextPage = () => { if (pageNum.value < pageCount.value) pageNum.value++ }
const prevPage = () => { if (pageNum.value > 1) pageNum.value-- }
const zoomIn  = () => { scale.value = Math.min(scale.value + 0.2, 4.0) }
const zoomOut = () => { scale.value = Math.max(scale.value - 0.2, 0.5) }

const condition = () => true
useHotkeys(['ArrowRight', 'l'], nextPage, { condition })
useHotkeys(['ArrowLeft', 'h'], prevPage, { condition })
useHotkeys(['+', '='], zoomIn, { condition })
useHotkeys(['-', '_'], zoomOut, { condition })

// ── Cleanup on unmount ───────────────────────────────────────────────────
onUnmounted(() => { cleanup() })
</script>

<template>
  <div class="flex-1 flex flex-col relative h-full">
    <!-- Toolbar -->
    <div class="h-[40px] flex items-center justify-between px-2 bg-gray-700 text-white shrink-0 z-10 shadow-md">
      <div class="flex items-center gap-1">
        <button @click="prevPage" :disabled="pageNum <= 1"
          class="p-1 hover:bg-gray-600 rounded disabled:opacity-30" title="Previous Page (Left Arrow)">
          <ChevronLeft :size="18" />
        </button>
        <span class="text-xs font-mono w-16 text-center select-none">{{ pageNum }} / {{ pageCount }}</span>
        <button @click="nextPage" :disabled="pageNum >= pageCount"
          class="p-1 hover:bg-gray-600 rounded disabled:opacity-30" title="Next Page (Right Arrow)">
          <ChevronRight :size="18" />
        </button>
      </div>

      <div class="flex items-center gap-2">
        <button @click="zoomOut" class="p-1 hover:bg-gray-600 rounded" title="Zoom Out (-)">
          <ZoomOut :size="16" />
        </button>
        <span class="text-xs w-8 text-center select-none">{{ Math.round(scale * 100) }}%</span>
        <button @click="zoomIn" class="p-1 hover:bg-gray-600 rounded" title="Zoom In (+)">
          <ZoomIn :size="16" />
        </button>
      </div>

      <div class="flex items-center gap-2 border-l border-gray-600 pl-2 ml-2">
        <button @click="invertColors = !invertColors"
          class="p-1 rounded flex items-center gap-1 text-[10px] uppercase font-bold transition-colors"
          :class="invertColors ? 'bg-blue-600 text-white' : 'hover:bg-gray-600 text-gray-300'"
          title="Dark Mode (Invert Colors)">
          <Moon :size="14" />
        </button>
      </div>
    </div>

    <!-- Canvas area -->
    <div class="flex-1 overflow-auto bg-gray-500/10 dark:bg-gray-900/50 flex justify-center p-4 relative">
      <!-- Loading spinner -->
      <div v-if="isLoading"
        class="absolute inset-0 flex items-center justify-center bg-gray-100/50 dark:bg-gray-900/50 z-10">
        <Loader2 class="animate-spin text-gray-500" :size="32" />
      </div>

      <!-- Error overlay -->
      <div v-if="error"
        class="absolute inset-0 flex flex-col items-center justify-center bg-gray-100 dark:bg-gray-900 z-10 p-4 text-center">
        <AlertCircle class="text-red-500 mb-2" :size="32" />
        <p class="text-sm font-bold text-red-600">PDF Render Error</p>
        <p class="text-xs text-gray-500 font-mono mt-1">{{ error }}</p>
      </div>

      <canvas
        v-show="!isLoading && !error"
        ref="canvasRef"
        class="shadow-lg transition-all duration-300 bg-white"
        :class="invertColors ? 'filter invert hue-rotate-180 brightness-90 contrast-90' : ''"
      />
    </div>
  </div>
</template>
