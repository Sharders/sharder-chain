<template>
    <div class="free-collar-drill">
        <p @click="$router.back()" class="mining-back">&lt;&lt;{{$t('mining.attribute.return_previous')}}</p>
        <div class="free-collar">
            <div class="free-header">
                <h1>{{$t('mining.free_collar_drill.free_collar_title')}}</h1>
                <p>{{$t('mining.free_collar_drill.free_collar_subtitle')}}</p>
            </div>
            <div class="free-list" v-for="free in freeList">
                <p>
                    <span>{{free.name}}</span>
                    <span>+{{free.num}}</span>
                    <span>{{$t('mining.create_history.diamond')}}</span>
                </p>
                <span class="state-info" v-if="free.state !== 'IN'">
                    <span>{{stateInfo(free.state)}}</span>
                </span>
                <button v-if="free.state === 'IN'" @click="toReceive(free)">{{$t('mining.free_collar_drill.collar')}}</button>
            </div>
        </div>
        <!--领取弹窗-->
        <ReceiveAlert :visible="isReceiveAlert" :alertInfo="alertInfo"></ReceiveAlert>
    </div>
</template>

<script>

    export default {
        name: "free-collar-drill",
        data() {
            return {
                freeList: [
                    {
                        name: this.$t('mining.free_collar_drill.daily_login'),
                        num: 100,
                        type: "ORDINARY",
                        typeInfo: "daily",
                        state: "GET",
                    },
                    {
                        name: this.$t('mining.free_collar_drill.registration_gift'),
                        num: 120,
                        type: "ORDINARY",
                        typeInfo: "registered",
                        state: "IN",
                    },
                    {
                        name: this.$t('mining.binding_validation.bind_phone'),
                        num: 140,
                        type: "BINDING",
                        typeInfo: "mobile-phone",
                        state: "IN",
                    },
                    {
                        name: this.$t('mining.binding_validation.bind_email'),
                        num: 160,
                        type: "BINDING",
                        typeInfo: "email",
                        state: "IN",
                    },
                ],
                alertInfo: {},
                isReceiveAlert: false,
            }
        }, methods: {
            stateInfo(val) {
                if (val === "GET") {
                    return this.$t('mining.free_collar_drill.received');
                }
            },
            toReceive(val) {
                if (val.type === 'BINDING') {
                    this.$router.push({name: 'binding-validation', params: val});
                }
                if (val.type === 'ORDINARY' && val.state === "IN") {
                    this.registered(val);
                }

            },
            registered(val) {
                //领取校验是否已经领取过了

                this.alertInfo = {title: this.$t('mining.binding_validation.receive_success_title'), content: this.$t('mining.binding_validation.receive_success_tip') + val.name + " +" + val.num + this.$t('mining.binding_validation.receive_success_tip2')};
                this.isReceiveAlert = true;
            },
        }
    }
</script>

<style scoped>
    .free-collar-drill .free-collar {
        padding: 30px 15px 0;
    }

    .free-collar .free-header {
        color: #333;
    }

    .free-collar .free-header h1 {
        font-size: 28px;
        font-weight: bold;
        padding: 20px 0 10px;
    }

    .free-collar .free-header p {
        font-size: 15px;
        padding: 0 0 20px 0;
    }

    .free-collar .free-list {
        background: #513acB;
        border-radius: 4px;
        margin: 10px 0 0 0;
        color: #fff;
        position: relative;
    }

    .free-collar .free-list p {
        padding: 26px 0 26px 20px;
    }

    .free-collar .free-list button {
        position: absolute;
        right: 10px;
        top: 16px;
        outline: none;
        border: none;
        background: #fff;
        border-radius: 4px;
        height: 40px;
        width: 100px;
        font-size: 13px;
        color: #333;
        cursor: pointer;
    }

    .free-collar .free-list button:active {
        background: #ffffffaa;
    }

    .free-list .state-info {
        display: inline-block;
        width: 70px;
        height: 70px;
        position: absolute;
        top: 0;
        right: 0;
        background: url("../../assets/img/state.png") no-repeat;
        border-top-right-radius: 6px;
    }

    .free-list .state-info span {
        font-size: 13px;
        display: inline-block;
        transform: rotate(45deg);
        position: absolute;
        top: 16px;
        right: 4px;
    }
</style>
