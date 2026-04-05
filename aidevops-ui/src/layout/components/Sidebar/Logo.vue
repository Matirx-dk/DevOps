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
        <h1 class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' && navType !== 3 ? variables.logoTitleColor : variables.logoLightTitleColor }">AIDevops</h1>
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
  background: transparent;
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    height: 56px;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    padding-left: 16px;
    gap: 10px;

    & .brand-badge {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      display: flex;
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
      flex-shrink: 0;
    }

    & .sidebar-title {
      margin: 0;
      padding: 0;
      color: #eaf2ff;
      font-weight: 600;
      font-size: 17px;
      letter-spacing: 0.5px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      line-height: 1;
    }
  }

  &.collapse {
    .sidebar-logo-link { justify-content: center; padding-left: 0; gap: 0; }
  }
}
</style>
