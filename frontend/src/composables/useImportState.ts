import { ref } from 'vue'

// Shared state between TemplateTriggerMenu and CategoryBatchImport
const pendingImportYaml = ref('')

export function useImportState() {
    const setImportYaml = (yaml: string) => {
        pendingImportYaml.value = yaml
    }

    const consumeImportYaml = () => {
        const val = pendingImportYaml.value
        pendingImportYaml.value = ''
        return val
    }

    return { pendingImportYaml, setImportYaml, consumeImportYaml }
}