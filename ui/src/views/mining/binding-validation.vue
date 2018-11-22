<template>
    <div class="binding-validation">
        <p @click="$router.back()" class="mining-back">&lt;&lt;返回上一页</p>
        <!--绑定手机-->
        <div v-if="binding.typeInfo === 'mobile-phone'" class="mobile-phone binding">
            <h1 class="title">绑定手机</h1>
            <p class="info">绑定手机后即可领取砖石奖励</p>
            <div class="input-div">
                <h3>手机号码</h3>
                <p>
                    <el-select v-model="value" placeholder="区号" class="select">
                        <el-option
                            v-for="item in options"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value">
                        </el-option>
                    </el-select>
                    <el-input v-model="phone" class="input" placeholder="请输入你的手机号码"></el-input>
                </p>
            </div>
            <div class="btn">
                <button @click="next()">下一步</button>
            </div>
        </div>
        <!--绑定邮箱-->
        <div v-if="binding.typeInfo === 'email'" class="email binding">
            <h1 class="title">绑定邮箱</h1>
            <p class="info">绑定邮箱后即可领取砖石奖励</p>
            <div class="input-div">
                <h3>邮箱号码</h3>
                <p>
                    <el-input v-model="phone" class="input" placeholder="请输入你的邮箱"></el-input>
                </p>
            </div>
            <div class="btn">
                <button @click="next()">下一步</button>
            </div>
        </div>
        <!--验证码-->
        <div v-if="isValidation" class="validation binding">
            <h1 class="title">请输入验证码</h1>
            <p class="info">验证码已发送到: {{phone||email}}请注意查收</p>
            <div class="input-div">
                <h3>请输入6位验证码</h3>
                <p>
                    <input v-model="validationCode" autocomplete="yes" class="input" maxlength="6"/>
                </p>
                <p class="resend">
                    重新发送验证码
                </p>
            </div>
            <div class="btn">
                <button @click="getRewards()">领取奖励</button>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "binding-validation",
        data() {
            return {
                binding: this.$route.params,
                isValidation: false,
                options: [
                    {
                        value: '086',
                        label: '+86',
                    },
                    {
                        value: '087',
                        label: '+87',
                    },
                    {
                        value: '088',
                        label: '+88'
                    },
                    {
                        value: '089',
                        label: '+89',
                    },
                ],
                value: '086',
                phone: '',
                email: '',
                validationCode: '',
            }
        },
        methods: {
            next() {
                this.isValidation = true;
            },
            getRewards() {

            },
        }, watch: {
            validationCode: function (val) {
                console.info(val);
            }
        }
    }
</script>

<style scoped>
    .mining-back {
        z-index: 9;
    }

    .binding-validation, .validation {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        overflow: auto;
        background: #fff;
    }

    .binding-validation .binding {
        padding: 30px 15px;
        color: #999;
        font-size: 14px;
    }

    .binding .title {
        font-size: 32px;
        font-weight: bold;
        padding: 20px 0 10px;
        color: #333;
    }

    .binding .info {
        padding: 0 0 50px;
    }

    .binding .input-div h3 {
        color: #333;
        font-weight: bold;
        padding: 0 0 15px;
    }

    .input-div .select {
        width: 74px;
    }

    .input-div .input {
        display: inline-block;
        float: right;
        width: calc(100% - 80px);
    }

    .input-div .resend {
        color: #513acB;
        cursor: pointer;
        display: inline-block;
    }

    .email .input-div .input {
        width: 100%;
    }

    .validation .input-div .input {
        width: 100%;
        padding: 10px 0 10px 25px;
        margin: 0 0 30px 0;
        letter-spacing: 40px;
        border: none;
        outline: none;
        font-size: 22px;
        font-weight: bold;
        color: #513acB;
        border-bottom: 1px solid #513acB;
    }

    .binding .btn button {
        cursor: pointer;
        border-radius: 4px;
        border: none;
        outline: none;
        background: #513acB;
        color: #fff;
        height: 45px;
        width: 100%;
        margin: 30px 0 0;
        font-size: 17px;
    }

    .binding .btn button:active {
        background: #513acBaa;
    }

    .validation {
        z-index: 2;
    }
</style>
