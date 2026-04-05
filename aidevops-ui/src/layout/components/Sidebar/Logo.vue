<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}" :style="{ backgroundColor: sideTheme === 'theme-dark' && navType !== 3 ? variables.menuBackground : variables.menuLightBackground }">
    <transition name="sidebarLogoFade">
      <router-link key="collapse" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo-img" />
        <div v-else class="brand-badge">Ai</div>
      </router-link>
    </transition>
  </div>
</template>

<script>
import variables from '@/assets/styles/variables.scss'
import logoImg from '@/assets/images/logo-aidevops.png'

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
      logo: logoImg
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
    justify-content: center;

    & .sidebar-logo-img {
      width: 44px;
      height: 44px;
      flex-shrink: 0;
    }

    & .brand-badge {
      width: 44px;
      height: 44px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 14px;
      font-weight: 700;
      background: linear-gradient(135deg, #3fa9ff 0%, #215cff 100%);
      box-shadow: 0 4px 12px rgba(33, 92, 255, 0.35);
      flex-shrink: 0;
    }
  }
}
</style>
