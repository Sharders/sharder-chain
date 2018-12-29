<template xmlns:v-clipboard="http://www.w3.org/1999/xhtml">
    <div class="invite-friends">
        <p @click="$router.back()" class="mining-back">&lt;&lt;{{$t('mining.attribute.return_previous')}}</p>
        <div class="invite">
            <h1 class="title">{{$t('mining.invite_friends.title')}}</h1>
            <p class="info">{{$t('mining.invite_friends.subtitle1')}}</p>
            <p class="info">{{$t('mining.invite_friends.subtitle2')}}</p>
            <div class="code">
                <p class="you-code">{{$t('mining.invite_friends.invite_code')}}</p>
                <p class="invite-code">
                    <span>{{inviteCode}}</span>
                </p>
                <p class="copy">
                    <button v-clipboard:copy="inviteCode" v-clipboard:success="copySuccess"
                            v-clipboard:error="copyError">{{$t('mining.invite_friends.copy_invite_code')}}{{$store.$fun}}
                    </button>
                </p>
                <div class="data">
                    <div>
                        <p>{{$t('mining.invite_friends.invited')}}</p>
                        <p>{{invitePeople}}{{$t('mining.invite_friends.unit_ren')}}</p>
                    </div>
                    <div>
                        <p>{{$t('mining.invite_friends.earn_rewards')}}</p>
                        <p>{{inviteReward}}{{$t('mining.create_history.diamond')}}</p>
                    </div>
                </div>
            </div>
            <div class="qr">
                <img src="../../assets/img/account.svg" class="img-qr">
                <p>{{$t('mining.invite_friends.qr_tip1')}}</p>
            </div>
            <p class="about">
                {{$t('mining.invite_friends.qr_tip2')}}
            </p>
        </div>
    </div>
</template>

<script>
    export default {
        name: "invite-friends",
        data() {
            return {
                inviteCode: "ABC1234",
                invitePeople: 8,
                inviteReward: 1600,
            }
        },
        methods: {
            copySuccess: function () {
                const _this = this;
                _this.$message({
                    showClose: true,
                    message: _this.$t('notification.clipboard_success'),
                    type: "success"
                });
            },
            copyError: function () {
                const _this = this;
                _this.$message({
                    showClose: true,
                    message: _this.$t('notification.clipboard_error'),
                    type: "error"

                });
            },
        }
    }
</script>

<style scoped>
    .invite-friends {
        background: #fff;
        text-align: center;
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        overflow: auto;
    }

    .invite .title {
        font-size: 27px;
        font-weight: bold;
        padding: 20px 0 15px;
        color: #513ac8;
    }

    .invite .info {
        color: #513ac8;
        font-size: 12px;
        margin: 0 0 5px 0;
        text-align: left;
        padding: 0 26px;
    }

    .invite .code, .invite .qr {
        width: calc(100% - 50px);
        margin: auto;
        background: #513ac8;
        color: #fff;
        border-radius: 4px;
    }

    .code .you-code {
        margin: 10px 0 0 0;
        padding: 30px 0 20px;
        font-size: 15px;
        font-weight: bold;
    }

    .code .invite-code span {
        display: inline-block;
        font-size: 32px;
        font-weight: bold;
        border-bottom: 2px solid #fff;
    }

    .code .copy button {
        outline: none;
        border: none;
        height: 35px;
        width: 140px;
        border-radius: 20px;
        background: #fff;
        color: #513ac8;
        font-size: 15px;
        font-weight: bold;
        margin: 25px 0;
    }

    .code .data {
        border-top: 1px solid #fff;
        position: relative;
    }

    .code .data:after {
        content: "";
        display: inline-block;
        width: 1px;
        height: 20px;
        background: #fff;
        position: absolute;
        left: 50%;
        top: calc(50% - 10px);
    }

    .code .data > div {
        display: inline-block;
        width: 49%;
        padding: 16px 0;
    }

    .code .data > div p {
        text-align: center;
        font-size: 15px;
    }

    .code .data > div p:last-child {
        font-weight: bold;
    }

    .invite .qr {
        margin: 15px auto 30px;
        padding: 10px 0 0 0;
    }

    .invite .qr .img-qr {
        width: 100px;
        height: 100px;
        background: #dbe2e8;
    }

    .invite .qr p {
        font-size: 12px;
        padding: 15px 0;
    }

    .invite .about {
        font-size: 11px;
        color: #513ac8;
        padding: 0 0 30px 0;
    }


</style>
