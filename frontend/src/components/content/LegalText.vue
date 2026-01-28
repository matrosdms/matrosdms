<script setup lang="ts">
import { ShieldAlert, Lock, Scale } from 'lucide-vue-next'
</script>

<template>
  <div class="space-y-6 text-sm text-gray-600 leading-relaxed font-sans">
    
    <!-- Header (Optional context, usually hidden in modal headers) -->
    <div class="border-b border-gray-100 pb-4 mb-4">
        <h3 class="text-gray-900 font-bold uppercase text-xs tracking-wider flex items-center gap-2">
            <Scale :size="16" class="text-blue-600"/> Terms of Use & Disclaimer
        </h3>
        <p class="text-xs text-gray-400 mt-1">Last Updated: 2025-01-01</p>
    </div>

    <!-- 1. Warranty -->
    <section>
        <h4 class="font-bold text-gray-900 mb-2 text-xs uppercase flex items-center gap-2">
            1. No Warranty
        </h4>
        <div class="bg-gray-50 p-4 rounded border border-gray-200 font-mono text-[11px] text-gray-500 text-justify">
            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
        </div>
    </section>

    <!-- 2. Liability -->
    <section>
        <h4 class="font-bold text-gray-900 mb-2 text-xs uppercase">2. Limitation of Liability</h4>
        <p>
            By using MatrosDMS, you acknowledge that you are solely responsible for the security, integrity, and backup of your data. The developers assume no responsibility for data loss, corruption, or any incidental or consequential damages resulting from the use or inability to use this software.
        </p>
    </section>

    <!-- 3. Security Warning -->
    <section class="bg-amber-50 border-l-4 border-amber-500 p-4 rounded-r-lg">
        <div class="flex items-start gap-3">
            <Lock class="text-amber-600 shrink-0 mt-0.5" :size="20" />
            <div>
                <h4 class="font-bold text-amber-800 text-xs uppercase mb-1">3. Security Notice: Deployment</h4>
                <p class="text-amber-900 mb-2 text-xs">
                    This application is designed for operation within a <strong>secure, private network (LAN/VPN)</strong>.
                </p>
                <p class="text-amber-800 text-[11px]">
                    <strong>Risk Acknowledgement:</strong> Direct exposure to the public internet without a properly configured Reverse Proxy (handling SSL/TLS) and additional Authentication layers is strictly discouraged. You assume full responsibility for securing the deployment environment.
                </p>
            </div>
        </div>
    </section>

    <div class="text-xs text-gray-400 pt-4 border-t border-gray-100">
        &copy; 2025 Schwehla IT. All rights reserved.
    </div>
  </div>
</template>