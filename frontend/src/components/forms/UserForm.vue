<script setup lang="ts">
import { ref, onMounted } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import { UserService } from '@/services/UserService'
import { push } from 'notivue'
import { useQueryClient } from '@tanstack/vue-query'
import { sha256 } from '@/lib/utils'
import { EUserRole, EUserRoleList, type EUserRoleType } from '@/enums'

const props = defineProps<{
initialData?: any
}>()
const emit = defineEmits(['close'])
const queryClient = useQueryClient()
const form = ref({
name: '',
firstname: '',
email: '',
password: '',
role: EUserRole.USER as EUserRoleType
})
const isLoading = ref(false)
const isEdit = !!props.initialData

onMounted(() => {
if (props.initialData) {
form.value = {
name: props.initialData.name || '',
firstname: props.initialData.firstname || '',
email: props.initialData.email || '',
role: (props.initialData.role as EUserRoleType) || EUserRole.USER,
password: '' 
}
}
})

const onSubmit = async () => {
if (!form.value.name) return push.warning('Username is required')
if (!isEdit && !form.value.password) return push.warning('Password is required')
isLoading.value = true
try {
const payload = { ...form.value }
if (payload.password) {
    payload.password = await sha256(payload.password)
} else if (isEdit) {
    delete (payload as any).password 
}

if (isEdit) {
    await UserService.update(props.initialData.uuid, payload)
    push.success('User updated')
} else {
    await UserService.create(payload)
    push.success('User created')
}

queryClient.invalidateQueries({ queryKey: ['users'] })
emit('close')
} catch (e: any) {
push.error(e.message)
} finally {
isLoading.value = false
}
}
</script>
<template>
  <BaseFormPanel :title="isEdit ? 'Edit User' : 'New User'" :is-loading="isLoading" @submit="onSubmit" @cancel="$emit('close')">
    <BaseInput v-model="form.name" label="Username" placeholder="Login Name" autofocus :disabled="isEdit" />
    <BaseInput v-model="form.firstname" label="Full Name" placeholder="Display Name" />
    <BaseInput v-model="form.email" label="Email" type="email" placeholder="user@company.com" />
    
    <BaseSelect v-model="form.role" label="Role">
        <option v-for="role in EUserRoleList" :key="role" :value="role">{{ role }}</option>
    </BaseSelect>
    
    <BaseInput v-model="form.password" label="Password" type="password" :placeholder="isEdit ? '(Leave empty to keep)' : '••••••••'" />
  </BaseFormPanel>
</template>