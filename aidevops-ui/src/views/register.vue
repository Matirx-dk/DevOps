<template>
  <div class="register">
    <div class="register-overlay"></div>
    <div class="register-panel">
      <div class="register-card">
        <div class="card-header">
          <div class="card-title-area">
            <h3 class="card-title">{{ title }}</h3>
          </div>
          <p class="card-subtitle">创建 AIDevOps 平台账号</p>
        </div>

        <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="card-form" @keyup.enter.native="handleRegister">
          <div class="form-field">
            <label class="field-label">账号</label>
            <el-form-item prop="username" style="margin-bottom:0">
              <el-input
                v-model.trim="registerForm.username"
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
                v-model="registerForm.password"
                type="password"
                show-password
                auto-complete="new-password"
                placeholder="请输入密码"
                size="large"
              >
                <svg-icon slot="prefix" icon-class="password" class="field-icon" />
              </el-input>
            </el-form-item>
          </div>

          <div class="form-field">
            <label class="field-label">确认密码</label>
            <el-form-item prop="confirmPassword" style="margin-bottom:0">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                show-password
                auto-complete="new-password"
                placeholder="请再次输入密码"
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
                  v-model.trim="registerForm.code"
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

          <el-button
            :loading="loading"
            size="large"
            type="primary"
            class="submit-btn"
            @click.native.prevent="handleRegister"
          >
            <span v-if="!loading">注 册</span>
            <span v-else>注册中...</span>
          </el-button>

          <div class="back-link">
            <router-link :to="'/login'">使用已有账户登录</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { getCodeImg, register } from "@/api/login"
import defaultSettings from '@/settings'

export default {
  name: "Register",
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.registerForm.password !== value) {
        callback(new Error("两次输入的密码不一致"))
      } else {
        callback()
      }
    }
    return {
      title: process.env.VUE_APP_TITLE,
      codeUrl: "",
      registerForm: {
        username: "",
        password: "",
        confirmPassword: "",
        code: "",
        uuid: ""
      },
      registerRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" },
          { min: 2, max: 20, message: '用户账号长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" },
          { min: 5, max: 20, message: "用户密码长度必须介于 5 和 20 之间", trigger: "blur" },
          { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\ |", trigger: "blur" }
        ],
        confirmPassword: [
          { required: true, trigger: "blur", message: "请再次输入您的密码" },
          { required: true, validator: equalToPassword, trigger: "blur" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      captchaEnabled: true
    }
  },
  created() {
    this.getCode()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.registerForm.uuid = res.uuid
        }
      })
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (valid) {
          this.loading = true
          register(this.registerForm).then(() => {
            this.$alert("恭喜你，注册成功！", '系统提示', {
              type: 'success'
            }).then(() => {
              this.$router.push("/login")
            }).catch(() => {})
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
.register {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40px 24px;
  background:
    radial-gradient(circle at 15% 25%, rgba(63, 130, 255, 0.20) 0%, transparent 35%),
    radial-gradient(circle at 85% 65%, rgba(33, 92, 255, 0.18) 0%, transparent 35%),
    linear-gradient(138deg, #060c18 0%, #0d1830 50%, #111e3a 100%);
  overflow: hidden;
}
.register-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, rgba(4, 8, 20, 0.72) 0%, rgba(4, 8, 20, 0.38) 55%, rgba(4, 8, 20, 0.12) 100%);
}
.register-panel {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
}
.register-card {
  width: 100%;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(20px);
  padding: 28px 28px 20px;
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.30),
    0 0 0 1px rgba(255, 255, 255, 0.50),
    inset 0 1px 0 rgba(255, 255, 255, 0.80);
}
.card-header {
  margin-bottom: 20px;
}
.card-title-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}
.card-title {
  margin: 0;
  color: #111827;
  font-size: 20px;
  font-weight: 700;
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
  &.is-disabled {
    opacity: 0.85;
  }
}
.back-link {
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

@media (max-width: 480px) {
  .register-card {
    padding: 24px 16px 16px;
    border-radius: 16px;
  }
  .captcha-row {
    flex-direction: column;
  }
  .captcha-img {
    width: 100%;
    height: 44px;
  }
}
</style>
