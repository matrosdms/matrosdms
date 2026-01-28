<script setup lang="ts">
import { AlertTriangle, ShieldAlert, Scale, Lock } from 'lucide-vue-next'
</script>

<template>
  <div class="h-full w-full bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-6 font-sans overflow-auto transition-colors">
    <div class="max-w-3xl w-full bg-white dark:bg-gray-800 rounded-xl shadow-xl border border-gray-200 dark:border-gray-700 overflow-hidden animate-in fade-in zoom-in-95 duration-300">
      
      <!-- Header -->
      <div class="bg-gray-900 dark:bg-black text-white p-8 text-center relative overflow-hidden">
        <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-red-500"></div>
        <div class="relative z-10 flex flex-col items-center">
            <Scale class="text-gray-300 mb-3 opacity-80" :size="48" stroke-width="1.5" />
            <h1 class="text-2xl font-bold tracking-tight uppercase">Legal Disclaimer & Terms of Use</h1>
            <p class="text-gray-400 text-sm mt-2">Please read carefully before using this software.</p>
        </div>
      </div>

      <!-- Content -->
      <div class="p-8 space-y-8 text-sm text-gray-600 dark:text-gray-300 leading-relaxed">
        
        <!-- 1. Warranty -->
        <section>
            <h3 class="text-gray-900 dark:text-gray-100 font-bold uppercase text-xs tracking-wider mb-2 flex items-center gap-2">
                <ShieldAlert :size="16" class="text-blue-600 dark:text-blue-400"/> No Warranty
            </h3>
            <div class="bg-gray-50 dark:bg-gray-900/50 p-4 rounded border border-gray-200 dark:border-gray-700 font-mono text-xs text-gray-500 dark:text-gray-400 text-justify">
                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
            </div>
        </section>

        <!-- 2. Liability -->
        <section>
            <h3 class="text-gray-900 dark:text-gray-100 font-bold uppercase text-xs tracking-wider mb-2">Limitation of Liability</h3>
            <p>
                By using MatrosDMS, you acknowledge that you are solely responsible for the security, integrity, and backup of your data. The developers assume no responsibility for data loss, corruption, or any incidental or consequential damages resulting from the use or inability to use this software.
            </p>
        </section>

        <!-- 3. Security Warning -->
        <section class="bg-amber-50 dark:bg-amber-900/20 border-l-4 border-amber-500 p-4 rounded-r-lg">
            <div class="flex items-start gap-3">
                <Lock class="text-amber-600 dark:text-amber-500 shrink-0 mt-0.5" :size="20" />
                <div>
                    <h3 class="font-bold text-amber-800 dark:text-amber-400 text-sm uppercase mb-1">Security Notice: Deployment Environment</h3>
                    <p class="text-amber-900 dark:text-amber-100 mb-2">
                        This application is designed for operation within a <strong>secure, private network (LAN/VPN)</strong>. It is not intended to be exposed directly to the public internet.
                    </p>
                    <p class="text-amber-800 dark:text-amber-200/80 text-xs">
                        <strong>Risk Acknowledgement:</strong> Direct exposure to the internet without a properly configured Reverse Proxy (e.g., Nginx, Traefik) handling SSL/TLS and additional Authentication layers may compromise your data. You assume full responsibility for securing the deployment environment.
                    </p>
                </div>
            </div>
        </section>

        <div class="text-center pt-6 border-t border-gray-100 dark:border-gray-700">
            <p class="text-xs text-gray-400 dark:text-gray-500">
                MatrosDMS &copy; 2025 Schwehla IT. All rights reserved.<br>
                Licensed for internal use.
            </p>
        </div>
      </div>

    </div>
  </div>
</template>