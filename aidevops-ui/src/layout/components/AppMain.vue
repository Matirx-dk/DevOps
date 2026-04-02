<template>
  <section class="app-main">
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view v-if="!$route.meta.link" :key="key" />
      </keep-alive>
    </transition>
    <iframe-toggle />
    <copyright />
  </section>
</template>

<script>
import copyright from "./Copyright/index"
import iframeToggle from "./IframeToggle/index"

export default {
  name: 'AppMain',
  components: { iframeToggle, copyright },
  computed: {
    cachedViews() {
      return this.$store.state.tagsView.cachedViews
    },
    key() {
      return this.$route.path
    }
  },
  watch: {
    $route() {
      this.addIframe()
    }
  },
  mounted() {
    this.addIframe()
  },
  methods: {
    addIframe() {
      const { name } = this.$route
      if (name && this.$route.meta.link) {
        this.$store.dispatch('tagsView/addIframeView', this.$route)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  min-height: calc(100vh - 56px);
  width: 100%;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #f5f7fb 0%, #f1f5fb 100%);

  &:fullscreen,
  &:-webkit-full-screen,
  &:-moz-full-screen,
  &:-ms-fullscreen {
    background: #fff;
    overflow-y: auto;
  }
}

.fixed-header + .app-main {
  overflow-y: auto;
  scrollbar-gutter: auto;
  height: calc(100vh - 56px);
  min-height: 0px;
}

.app-main:has(.copyright) {
  padding-bottom: 36px;
}

.fixed-header + .app-main {
  margin-top: 56px;
}

.hasTagsView {
  .app-main {
    min-height: calc(100vh - 90px);
  }

  .fixed-header + .app-main {
    margin-top: 90px;
    height: calc(100vh - 90px);
    min-height: 0px;
  }
}

@media screen and (max-width: 991px) {
  .fixed-header + .app-main {
    padding-bottom: max(60px, calc(constant(safe-area-inset-bottom) + 40px));
    padding-bottom: max(60px, calc(env(safe-area-inset-bottom) + 40px));
    overscroll-behavior-y: none;
  }

  .hasTagsView .fixed-header + .app-main {
    padding-bottom: max(60px, calc(constant(safe-area-inset-bottom) + 40px));
    padding-bottom: max(60px, calc(env(safe-area-inset-bottom) + 40px));
    overscroll-behavior-y: none;
  }
}
</style>

<style lang="scss">
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background-color: #eef3f8;
}

::-webkit-scrollbar-thumb {
  background-color: #c3cfdd;
  border-radius: 3px;
}
</style>
