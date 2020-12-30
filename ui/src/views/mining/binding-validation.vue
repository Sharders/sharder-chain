<template>
    <div class="binding-validation">
        <p @click="$router.back()" class="mining-back">{{$t('mining.attribute.return_previous')}}</p>
        <!--绑定手机-->
        <div v-if="binding.typeInfo === 'mobile-phone'" class="mobile-phone binding">
            <h1 class="title">{{$t('mining.binding_validation.bind_phone')}}</h1>
            <p class="info">{{$t('mining.binding_validation.bind_phone_tip')}}</p>
            <div class="input-div">
                <h3>{{$t('mining.binding_validation.bind_phone_tip')}}</h3>
                <p>
                    <el-select v-model="value" :placeholder="$t('mining.binding_validation.area_code')" class="select">
                        <el-option
                            v-for="item in options"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value">
                        </el-option>
                    </el-select>
                    <el-input v-model="phone" class="input" :placeholder="$t('mining.binding_validation.phone_input_tip')"></el-input>
                </p>
            </div>
            <div class="btn">
                <button @click="next()">{{$t('mining.binding_validation.next_step')}}</button>
            </div>
        </div>
        <!--绑定邮箱-->
        <div v-if="binding.typeInfo === 'email'" class="email binding">
            <h1 class="title">{{$t('mining.binding_validation.bind_email')}}</h1>
            <p class="info">{{$t('mining.binding_validation.bind_email_tip')}}</p>
            <div class="input-div">
                <h3>{{$t('mining.binding_validation.email_number')}}</h3>
                <p>
                    <el-input v-model="phone" class="input" :placeholder="$t('mining.binding_validation.email_number_tip')"></el-input>
                </p>
            </div>
            <div class="btn">
                <button @click="next()">{{$t('mining.binding_validation.next_step')}}</button>
            </div>
        </div>
        <!--验证码-->
        <div v-if="isValidation" class="validation binding">
            <h1 class="title">{{$t('mining.binding_validation.verification_title')}}</h1>
            <p class="info">{{$t('mining.binding_validation.verification_tip1')}}{{phone||email}}{{$t('mining.binding_validation.verification_tip2')}}</p>
            <div class="input-div">
                <h3>{{$t('mining.binding_validation.verification_tip3')}}</h3>
                <p>
                    <input v-model="validationCode" autocomplete="yes" class="input" maxlength="6"/>
                </p>
                <p class="resend">
                    {{$t('mining.binding_validation.resend_verification')}}
                </p>
            </div>
            <div class="btn">
                <button @click="getRewards()">{{$t('mining.binding_validation.receive_award')}}</button>
            </div>
        </div>
        <!--领取弹窗-->
        <ReceiveAlert :visible="isReceiveAlert" :alertInfo="alertInfo" @callback="$router.back()"></ReceiveAlert>
    </div>
</template>

<script>

    export default {
        name: "binding-validation",
        data() {
            return {
                isReceiveAlert: false,
                alertInfo: {},
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
                if (this.phone || this.email) {
                    this.isValidation = true;
                }
            },
            getRewards() {
                if (this.validationCode.length !== 6) {
                    return;
                }
                //校验验证码

                this.alertInfo = {title: this.$t('mining.binding_validation.receive_success_title'), content: this.$t('mining.binding_validation.receive_success_tip') + this.binding.name + " +" + this.binding.num + this.$t('mining.binding_validation.receive_success_tip2')};
                this.isReceiveAlert = true;
            },
        }, watch: {
            validationCode: function (val) {
                if (val.length === 6) {
                    this.getRewards();
                }
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
        color: #555;
    }

    .binding .info {
        padding: 0 0 50px;
    }

    .binding .input-div h3 {
        color: #555;
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
        color: #3fb09a;
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
        color: #3fb09a;
        border-bottom: 1px solid #3fb09a;
    }

    .binding .btn button {
        cursor: pointer;
        border-radius: 4px;
        border: none;
        outline: none;
        background: #3fb09a;
        color: #fff;
        height: 45px;
        width: 100%;
        margin: 30px 0 0;
        font-size: 17px;
    }

    .binding .btn button:active {
        background: #3fb09aaa;
    }

    .validation {
        z-index: 2;
    }
</style>
