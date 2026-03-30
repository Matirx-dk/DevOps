<template>
  <div>
    <svg-icon icon-class="question" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'AIDevOpsDoc',
  data() {
    return {
      url: 'https://docs.openclaw.ai'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
