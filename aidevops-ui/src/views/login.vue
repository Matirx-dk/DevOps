<template>
  <div class="login">
    <div class="login-overlay"></div>
    <div class="login-side">
      <div class="brand-mark">AIOps</div>
      <h1 class="brand-title">AIDevOps 智能运维平台</h1>
      <p class="brand-desc">聚焦发布、质量、监控与运维协同，让测试环境与交付链路更清晰、更稳定。</p>
      <div class="brand-tags">
        <span>CI/CD</span>
        <span>Kubernetes</span>
        <span>Harbor</span>
        <span>Observability</span>
      </div>
      <div class="brand-metrics">
        <div class="metric-card">
          <strong>发布链路</strong>
          <span>构建、推送、部署统一入口</span>
        </div>
        <div class="metric-card">
          <strong>运行状态</strong>
          <span>监控、告警、AI 对话快速联动</span>
        </div>
      </div>
    </div>

    <div class="login-panel">
      <div class="login-card">
        <div class="card-header">
          <div class="card-logo">
            <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="40" height="40" rx="12" fill="url(#logoGrad)"/>
              <path d="M12 20L17 25L28 14" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
              <defs>
                <linearGradient id="logoGrad" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#3fa9ff"/>
                  <stop offset="1" stop-color="#215cff"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div class="card-title-area">
            <h3 class="card-title">{{ title }}</h3>
            <span class="env-badge" :class="envLabel.toLowerCase()">{{ envLabel }}</span>
          </div>
          <p class="card-subtitle">欢迎登录 AIDevOps 平台</p>
        </div>

        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="card-form" @keyup.enter.native="handleLogin">
          <div class="form-field">
            <label class="field-label">账号</label>
            <el-form-item prop="username" style="margin-bottom:0">
              <el-input
                v-model.trim="loginForm.username"
                type="text"
                auto-complete="username"
                placeholder="请输入账号"
                size="large"
              >
                <svg-icon slot="prefix" icon-class="user" class="field-icon" />
              </el-input>
            </el-form-item>
          </div>

          <div class="form-field">
            <label class="field-label">密码</label>
            <el-form-item prop="password" style="margin-bottom:0">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                auto-complete="current-password"
                placeholder="请输入密码"
                size="large"
              >
                <svg-icon slot="prefix" icon-class="password" class="field-icon" />
              </el-input>
            </el-form-item>
          </div>

          <div class="form-field" v-if="captchaEnabled">
            <label class="field-label">验证码</label>
            <el-form-item prop="code" style="margin-bottom:0">
              <div class="captcha-row">
                <el-input
                  v-model.trim="loginForm.code"
                  auto-complete="off"
                  placeholder="请输入验证码"
                  size="large"
                  class="captcha-input"
                >
                  <svg-icon slot="prefix" icon-class="validCode" class="field-icon" />
                </el-input>
                <div class="captcha-img" @click="getCode" title="点击刷新">
                  <img :src="codeUrl" alt="验证码"/>
                </div>
              </div>
            </el-form-item>
          </div>

          <div class="form-row">
            <el-checkbox v-model="loginForm.rememberMe" class="remember-check">记住密码</el-checkbox>
            <span class="form-hint">建议使用浏览器自动填充</span>
          </div>

          <div class="security-note">
            <i class="el-icon-lock"></i>
            <span>账号信息仅用于当前平台认证</span>
          </div>

          <el-button
            :loading="loading"
            :disabled="loading"
            size="large"
            type="primary"
            class="submit-btn"
            @click.native.prevent="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登录中...</span>
          </el-button>

          <div class="register-link" v-if="register">
            <router-link :to="'/register'">还没有账号？立即注册</router-link>
          </div>
        </el-form>
      </div>

      <div class="login-footer">
        <span>{{ footerContent }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'
import defaultSettings from '@/settings'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      captchaEnabled: true,
      register: false,
      redirect: undefined
    }
  },
  computed: {
    envLabel() {
      const host = window.location.hostname || ''
      if (host.includes('test.')) return 'TEST'
      if (host.includes('devops.')) return 'CLOUD'
      return 'AIDevOps'
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        ...this.loginForm,
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : rememberMe === 'true'
      }
    },
    handleLogin() {
      if (this.loading) return
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 })
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("username")
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{})
          }).catch(() => {
            this.loading = false
            this.loginForm.code = ''
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
  min-height: 100vh;
  padding: 0 6%;
  background:
    radial-gradient(circle at 15% 25%, rgba(63, 130, 255, 0.20) 0%, transparent 35%),
    radial-gradient(circle at 85% 65%, rgba(33, 92, 255, 0.18) 0%, transparent 35%),
    linear-gradient(138deg, #060c18 0%, #0d1830 50%, #111e3a 100%);
  overflow: hidden;
}
.login-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, rgba(4, 8, 20, 0.72) 0%, rgba(4, 8, 20, 0.38) 55%, rgba(4, 8, 20, 0.12) 100%);
}
.login-side,
.login-panel {
  position: relative;
  z-index: 1;
}
.login-side {
  max-width: 500px;
  color: #e4efff;
  flex-shrink: 0;
}
.brand-mark {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  margin-bottom: 16px;
  border-radius: 999px;
  background: rgba(63, 169, 255, 0.12);
  border: 1px solid rgba(100, 180, 255, 0.20);
  color: #8fd3ff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}
.brand-title {
  margin: 0 0 14px;
  font-size: 42px;
  line-height: 1.16;
  font-weight: 800;
  color: #fff;
}
.brand-desc {
  margin: 0;
  color: rgba(220, 235, 255, 0.72);
  font-size: 15px;
  line-height: 1.85;
  max-width: 460px;
}
.brand-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}
.brand-tags span {
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.10);
  color: #c8d9f5;
  font-size: 13px;
}
.brand-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 32px;
  max-width: 480px;
}
.metric-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.10);
  backdrop-filter: blur(8px);
}
.metric-card strong {
  display: block;
  margin-bottom: 7px;
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
}
.metric-card span {
  display: block;
  color: rgba(210, 228, 255, 0.70);
  font-size: 13px;
  line-height: 1.7;
}

.login-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  max-width: 440px;
  padding: 40px 0 32px;
}
.login-card {
  width: 100%;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(20px);
  padding: 36px 32px 24px;
  box-shadow:
    0 32px 80px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.50),
    inset 0 1px 0 rgba(255, 255, 255, 0.80);
}
.card-header {
  margin-bottom: 28px;
}
.card-logo {
  width: 44px;
  height: 44px;
  margin-bottom: 16px;
  svg {
    width: 44px;
    height: 44px;
  }
}
.card-title-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.card-title {
  margin: 0;
  color: #111827;
  font-size: 22px;
  font-weight: 700;
}
.env-badge {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
}
.env-badge.test {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}
.env-badge.cloud {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}
.env-badge.aidevops {
  background: rgba(33, 92, 255, 0.10);
  color: #215cff;
}
.card-subtitle {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.card-form {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.form-field {
  margin-bottom: 18px;
}
.field-label {
  display: block;
  margin-bottom: 7px;
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}
.field-icon {
  width: 14px;
  height: 14px;
  color: #9ca3af;
  margin-left: 2px;
}
::v-deep .el-input {
  &.el-input--large {
    .el-input__inner {
      height: 46px;
      border-radius: 12px;
      border: 1.5px solid #e5e7eb;
      background: #fafbfc;
      color: #111827;
      font-size: 14px;
      transition: border-color .2s, box-shadow .2s, background .2s;
      &::placeholder {
        color: #adb5bd;
      }
      &:hover {
        border-color: #d1d5db;
        background: #fff;
      }
      &:focus {
        border-color: #3fa9ff;
        background: #fff;
        box-shadow: 0 0 0 3px rgba(63, 169, 255, 0.12);
        outline: none;
      }
    }
    .el-input__prefix {
      left: 12px;
    }
    .el-input__inner {
      padding-left: 36px;
    }
  }
}
::v-deep .el-form-item.is-error .el-input__inner {
  border-color: #f56c6c;
  box-shadow: none;
}
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: stretch;
}
.captcha-input {
  flex: 1;
}
.captcha-img {
  flex: 0 0 108px;
  height: 46px;
  border-radius: 12px;
  overflow: hidden;
  border: 1.5px solid #e5e7eb;
  cursor: pointer;
  transition: border-color .2s;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  &:hover {
    border-color: #3fa9ff;
  }
}
.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.remember-check {
  ::v-deep .el-checkbox__label {
    color: #6b7280;
    font-size: 13px;
  }
}
.form-hint {
  color: #9ca3af;
  font-size: 12px;
}
.security-note {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 20px;
  padding: 9px 12px;
  border-radius: 10px;
  background: rgba(37, 99, 235, 0.05);
  color: #8b95a8;
  font-size: 12px;
  i {
    color: #3fa9ff;
    font-size: 12px;
    flex-shrink: 0;
  }
}
.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3fa9ff 0%, #1d5fed 100%);
  border: none;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  box-shadow: 0 8px 24px rgba(33, 92, 255, 0.28);
  transition: transform .15s, box-shadow .15s, opacity .15s;
  &:hover:not(.is-disabled) {
    transform: translateY(-1px);
    box-shadow: 0 12px 30px rgba(33, 92, 255, 0.34);
  }
  &:active:not(.is-disabled) {
    transform: translateY(0);
  }
  &.is-disabled {
    opacity: 0.85;
  }
}
.register-link {
  text-align: center;
  margin-top: 14px;
  a {
    color: #3fa9ff;
    font-size: 13px;
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }
}
.login-footer {
  margin-top: 24px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
  letter-spacing: 0.5px;
}

@media (max-width: 1100px) {
  .login {
    justify-content: center;
    padding: 0 24px 60px;
    gap: 0;
  }
  .login-side {
    display: none;
  }
  .login-panel {
    max-width: 420px;
    width: 100%;
    padding-top: 48px;
  }
}
@media (max-width: 540px) {
  .login-card {
    padding: 28px 20px 20px;
    border-radius: 20px;
  }
  .card-title {
    font-size: 20px;
  }
  .captcha-row {
    flex-direction: column;
  }
  .captcha-img {
    flex: 0 0 auto;
    width: 100%;
    height: 44px;
  }
}
</style>
