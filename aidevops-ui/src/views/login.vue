<template>
  <div class="login">
    <div class="login-panel">
      <div class="login-brand">
        <div class="brand-icon">A</div>
        <div>
          <h3 class="title">{{ title }}</h3>
          <p class="subtitle">AIDevOps 智能运维平台 · 测试环境</p>
        </div>
      </div>

      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
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
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="请输入密码"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled">
          <el-input
            v-model="loginForm.code"
            auto-complete="off"
            placeholder="请输入验证码"
            style="width: 63%"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
          </el-input>
          <div class="login-code">
            <img :src="codeUrl" @click="getCode" class="login-code-img"/>
          </div>
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
          <span class="env-tag">TEST</span>
        </div>

        <el-form-item style="width:100%; margin-bottom: 0;">
          <el-button
            :loading="loading"
            size="medium"
            type="primary"
            class="login-btn"
            @click.native.prevent="handleLogin"
          >
            <span v-if="!loading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
          <div style="float: right; margin-top: 12px;" v-if="register">
            <router-link class="link-type" :to="'/register'">立即注册</router-link>
          </div>
        </el-form-item>
      </el-form>
    </div>

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
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 24px;
  background: linear-gradient(180deg, #f4f7fb 0%, #eef3fb 100%);
}
.login-panel {
  width: 420px;
  padding: 32px 30px 28px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 10px 32px rgba(31, 45, 61, 0.08);
  border: 1px solid #e8eef7;
}
.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}
.brand-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #409eff 0%, #2f6bff 100%);
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.25);
}
.title {
  margin: 0;
  color: #1f2d3d;
  font-size: 24px;
  font-weight: 700;
}
.subtitle {
  margin: 6px 0 0;
  color: #7a8aa0;
  font-size: 13px;
}
.login-form {
  .el-input {
    height: 40px;
    input {
      height: 40px;
      border-radius: 8px;
    }
  }
  .input-icon {
    height: 40px;
    width: 14px;
    margin-left: 2px;
  }
}
.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 22px;
}
.env-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: #edf5ff;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}
.login-code {
  width: 33%;
  height: 40px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 8px;
  }
}
.login-btn {
  width: 100%;
  height: 40px;
  border-radius: 8px;
}
.login-code-img {
  height: 40px;
}
.el-login-footer {
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 40px;
  line-height: 40px;
  text-align: center;
  color: #8a97ab;
  font-size: 12px;
}
</style>
