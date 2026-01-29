import { ref } from 'vue'

// Singleton state to bridge the "Load Template" button (Col 2) 
// and the "Editor" (Col 3) without complex prop drilling in generic views.
const pendingImportYaml = ref<string>('')

export function useImportState() {
    const setImportYaml = (yaml: string) => {
        pendingImportYaml.value = yaml
    }

    const consumeImportYaml = (): string | null => {
        if (!pendingImportYaml.value) return null
        const val = pendingImportYaml.value
        pendingImportYaml.value = '' // Reset after consumption
        return val
    }

    return {
        pendingImportYaml,
        setImportYaml,
        consumeImportYaml
    }
}