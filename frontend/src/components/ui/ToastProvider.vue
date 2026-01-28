<script setup lang="ts">
import { Notivue, Notifications, filledIcons, pastelTheme } from 'notivue'
</script>

<template>
  <Notivue v-slot="item">
    <Notifications :item="item" :icons="filledIcons" :theme="pastelTheme" />
  </Notivue>
</template>