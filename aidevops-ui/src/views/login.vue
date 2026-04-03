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
    </div>
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="login-header">
        <div class="logo-dot"></div>
        <div class="login-header-main">
          <div class="login-header-top">
            <h3 class="title">{{ title }}</h3>
            <span class="env-tag">{{ envLabel }}</span>
          </div>
          <p class="subtitle">欢迎登录 AIDevOps 平台</p>
          <p class="form-tip">统一入口，处理发布、监控、运维与 AI 对话能力。</p>
        </div>
      </div>
      <el-form-item prop="username">
        <div class="field-label">账号</div>
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          placeholder="请输入账号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <div class="field-label">密码</div>
        <el-input
          v-model="loginForm.password"
          type="password"
          show-password
          auto-complete="off"
          placeholder="请输入密码"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled" class="captcha-item">
        <div class="field-label">验证码</div>
        <div class="captcha-row">
          <el-input
            v-model="loginForm.code"
            auto-complete="off"
            placeholder="请输入验证码"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
          </el-input>
          <div class="login-code">
            <img :src="codeUrl" @click="getCode" class="login-code-img"/>
          </div>
        </div>
      </el-form-item>
      <div class="login-options">
        <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
        <span class="login-hint">建议使用浏览器自动填充提高效率</span>
      </div>
      <el-form-item style="width:100%; margin-bottom: 10px;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          class="login-btn"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登录平台</span>
          <span v-else>登录中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
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
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
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
  min-height: 100%;
  padding: 0 8%;
  background:
    radial-gradient(circle at 20% 20%, rgba(63, 169, 255, 0.24), transparent 30%),
    radial-gradient(circle at 80% 30%, rgba(33, 92, 255, 0.22), transparent 28%),
    linear-gradient(135deg, #0b1220 0%, #131d33 45%, #18284b 100%);
  overflow: hidden;
}
.login-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(7, 15, 30, 0.78) 0%, rgba(7, 15, 30, 0.45) 45%, rgba(7, 15, 30, 0.18) 100%);
}
.login-side,
.login-form,
.el-login-footer {
  position: relative;
  z-index: 1;
}
.login-side {
  max-width: 520px;
  color: #eaf2ff;
}
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  height: 34px;
  padding: 0 16px;
  margin-bottom: 18px;
  border-radius: 999px;
  background: rgba(63, 169, 255, 0.16);
  border: 1px solid rgba(63, 169, 255, 0.3);
  color: #8fd3ff;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
}
.brand-title {
  margin: 0 0 16px;
  font-size: 38px;
  line-height: 1.2;
  font-weight: 700;
}
.brand-desc {
  margin: 0;
  color: rgba(234, 242, 255, 0.78);
  font-size: 15px;
  line-height: 1.9;
}
.brand-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}
.brand-tags span {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #dce9ff;
  font-size: 13px;
}
.title {
  margin: 0;
  color: #1c2742;
  font-size: 28px;
  font-weight: 700;
}
.subtitle {
  margin: 6px 0 0;
  color: #4d5b78;
  font-size: 14px;
  font-weight: 600;
}
.form-tip {
  margin: 8px 0 0;
  color: #7b879d;
  font-size: 12px;
  line-height: 1.7;
}
.login-header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 28px;
}
.login-header-main {
  flex: 1;
}
.login-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.logo-dot {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, #3fa9ff 0%, #215cff 100%);
  box-shadow: 0 12px 28px rgba(33, 92, 255, 0.28);
}
.login-form {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(10px);
  width: 440px;
  padding: 34px 32px 20px;
  box-shadow: 0 20px 56px rgba(0, 0, 0, 0.18);
  border: 1px solid rgba(255,255,255,0.5);
  .el-input {
    height: 46px;
    input {
      height: 46px;
      border-radius: 12px;
      border-color: #dbe5f3;
      background: #f8fbff;
    }
    input:focus {
      border-color: #3fa9ff;
      background: #fff;
    }
  }
  .input-icon {
    height: 46px;
    width: 14px;
    margin-left: 2px;
  }
}
.field-label {
  margin-bottom: 8px;
  color: #41506d;
  font-size: 12px;
  font-weight: 600;
}
.captcha-item ::v-deep .el-form-item__content {
  display: block;
}
.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.captcha-row .el-input {
  flex: 1;
}
.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 22px;
}
.login-hint {
  color: #8a94a8;
  font-size: 12px;
}
.env-tag {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 999px;
  background: #eef4ff;
  color: #215cff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .5px;
  white-space: nowrap;
}
.login-code {
  width: 118px;
  height: 46px;
  flex: 0 0 118px;
  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 12px;
    width: 118px;
    height: 46px;
    object-fit: cover;
    border: 1px solid #dbe5f3;
    background: #fff;
  }
}
.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3fa9ff 0%, #215cff 100%);
  border: none;
  box-shadow: 0 12px 28px rgba(33, 92, 255, 0.22);
  font-weight: 600;
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  left: 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.72);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 42px;
}
@media (max-width: 1080px) {
  .login {
    justify-content: center;
    padding: 32px 20px 72px;
  }
  .login-side {
    display: none;
  }
}

@media (max-width: 640px) {
  .login {
    padding: 20px 14px 72px;
  }
  .login-form {
    width: 100%;
    max-width: 100%;
    padding: 26px 18px 18px;
    border-radius: 18px;
  }
  .login-header-top {
    align-items: flex-start;
    flex-direction: column;
  }
  .captcha-row {
    flex-direction: column;
    align-items: stretch;
  }
  .login-code {
    width: 100%;
    flex: none;
  }
  .login-code img {
    width: 100%;
  }
  .login-options {
    align-items: flex-start;
    gap: 10px;
    flex-direction: column;
  }
  .el-login-footer {
    padding: 0 16px;
    line-height: 1.6;
  }
}
</style>
