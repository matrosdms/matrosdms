<template>
  <div class="p-6 max-w-6xl mx-auto">
    <h1 class="text-2xl font-bold mb-6">Category Setup Wizard</h1>

    <!-- 1. Dimension Selector -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
      
      <!-- Left: Controls -->
      <div class="card bg-white p-4 shadow rounded">
        <h3 class="font-semibold mb-3 text-gray-700">1. Select Target</h3>
        
        <label class="block text-sm font-medium text-gray-700 mb-1">Dimension</label>
        <select v-model="selectedDimension" class="w-full border p-2 rounded mb-4">
          <option :value="ERootCategory.WHAT">WHAT (Topics/Content)</option>
          <option :value="ERootCategory.WHO">WHO (Persons/Groups)</option>
          <option :value="ERootCategory.WHERE">WHERE (Locations)</option>
          <option :value="ERootCategory.KIND">KIND (Doc Types)</option>
        </select>

        <div class="bg-blue-50 border border-blue-200 text-blue-800 text-xs p-3 rounded mt-4">
            <p class="font-bold mb-1">Manual Mode (YAML)</p>
            <p>Define your category tree using standard YAML syntax. Use 2 spaces for indentation.</p>
        </div>
      </div>

      <!-- Right: Editor -->
      <div class="col-span-2 card bg-white p-4 shadow rounded flex flex-col h-[600px]">
        <h3 class="font-semibold mb-2 text-gray-700">2. Customize Structure</h3>
        <div class="bg-gray-100 text-xs p-2 mb-2 rounded border border-gray-200 flex justify-between">
          <span><strong>Format:</strong> YAML</span>
          <span>Tip: Use 2 spaces for indentation.</span>
        </div>
        
        <textarea 
          v-model="editorContent" 
          class="flex-1 w-full font-mono text-sm p-4 border rounded bg-gray-50 focus:bg-white focus:ring-2 focus:ring-blue-500 outline-none resize-none"
          spellcheck="false"
          placeholder="- Finance:&#10;    - Invoices:&#10;        - Incoming&#10;    - Taxes&#10;- HR:&#10;    - Contracts"
        ></textarea>

        <!-- Actions -->
        <div class="flex justify-between items-center mt-4">
          <div class="text-sm">
            <span v-if="simulationStatus === 'success'" class="text-green-600 font-bold">✓ Structure Valid</span>
            <span v-if="simulationStatus === 'error'" class="text-red-600 font-bold">⚠ Validation Failed</span>
          </div>
          <div class="flex gap-3">
            <button 
              @click="runSimulation" 
              class="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600 transition"
              :disabled="isProcessing"
            >
              Verify / Check
            </button>
            <button 
              @click="applyStructure" 
              class="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="simulationStatus !== 'success' || isProcessing"
            >
              Save Structure
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Error Modal -->
    <div v-if="errorMessage" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white p-6 rounded shadow-xl max-w-md">
        <h3 class="text-xl font-bold text-red-600 mb-2">Import Failed</h3>
        <p class="text-gray-700 mb-4 whitespace-pre-wrap text-sm">{{ errorMessage }}</p>
        <button @click="errorMessage = null" class="w-full bg-gray-200 py-2 rounded hover:bg-gray-300">Close</button>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CategoryService } from '@/services/CategoryService';
import { ERootCategory, type ERootCategoryType } from '@/enums';

// State
const selectedDimension = ref<ERootCategoryType>(ERootCategory.WHAT);
const editorContent = ref('');

// UX State
const isProcessing = ref(false);
const simulationStatus = ref<'idle' | 'success' | 'error'>('idle');
const errorMessage = ref<string | null>(null);

const runSimulation = async () => {
  isProcessing.value = true;
  errorMessage.value = null;
  try {
    // True = Simulate Only, False = Replace
    await CategoryService.importToRoot(selectedDimension.value, editorContent.value, true, false);
    simulationStatus.value = 'success';
  } catch (e: any) {
    simulationStatus.value = 'error';
    handleError(e);
  } finally {
    isProcessing.value = false;
  }
};

const applyStructure = async () => {
  if(!confirm("This will update your category tree. Continue?")) return;
  
  isProcessing.value = true;
  try {
    // False = Commit, False = Replace (Merge Mode for safety)
    await CategoryService.importToRoot(selectedDimension.value, editorContent.value, false, false);
    alert("Success! Structure updated.");
  } catch (e: any) {
    handleError(e);
  } finally {
    isProcessing.value = false;
  }
};

const handleError = (e: any) => {
  // STRICT MODE: Check errorCode instead of string matching
  if (e.errorCode === 'PROC_101' || e.status === 409) {
    errorMessage.value = "Target is not empty. To overwrite completely, use the full Settings > Import tool with 'Replace' mode enabled.";
  } else if (e.errorCode === 'VAL_100') {
    errorMessage.value = "Syntax Error:\n" + e.message;
  } else {
    errorMessage.value = e.message || "An unexpected error occurred.";
  }
};
</script>