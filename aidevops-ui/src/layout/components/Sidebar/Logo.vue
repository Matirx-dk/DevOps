<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}" :style="{ backgroundColor: sideTheme === 'theme-dark' && navType !== 3 ? variables.menuBackground : variables.menuLightBackground }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <div class="brand-badge" v-if="logoText">{{ logoText }}</div>
        <img v-else-if="logo" :src="logo" class="sidebar-logo" />
        <h1 v-else class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' && navType !== 3 ? variables.logoTitleColor : variables.logoLightTitleColor }">{{ title }}</h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <div class="brand-badge" v-if="logoText">{{ logoText }}</div>
        <img v-else-if="logo" :src="logo" class="sidebar-logo" />
        <h1 class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' && navType !== 3 ? variables.logoTitleColor : variables.logoLightTitleColor }">{{ title }}</h1>
      </router-link>
    </transition>
  </div>
</template>

<script>
import variables from '@/assets/styles/variables.scss'

export default {
  name: 'SidebarLogo',
  props: {
    collapse: { type: Boolean, required: true }
  },
  computed: {
    variables() { return variables },
    sideTheme() { return this.$store.state.settings.sideTheme },
    navType() { return this.$store.state.settings.navType }
  },
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      logo: '',
      logoText: 'A'
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active { transition: opacity 1.5s; }
.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to { opacity: 0; }

.sidebar-logo-container {
  position: relative;
  height: 56px;
  line-height: 56px;
  background: transparent;
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 10px;

    & .brand-badge {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 18px;
      font-weight: 700;
      background: linear-gradient(135deg, #3fa9ff 0%, #215cff 100%);
      box-shadow: 0 10px 24px rgba(33, 92, 255, 0.28);
      flex-shrink: 0;
    }

    & .sidebar-logo {
      width: 32px;
      height: 32px;
      vertical-align: middle;
      margin-right: 12px;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: #fff;
      font-weight: 700;
      line-height: 56px;
      font-size: 14px;
      letter-spacing: 0.2px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .brand-badge,
    .sidebar-logo { margin-right: 0; }
    .sidebar-logo-link { gap: 0; }
  }
}
</style>
