<template>
  <div>
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'AIDevOpsGit',
  data() {
    return {
      url: 'https://github.com/dekangzou/DevOps'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
