<template xmlns:v-clipboard="http://www.w3.org/1999/xhtml">
    <div class="invite-friends">
        <p @click="$router.back()" class="mining-back">&lt;&lt;返回上一页</p>
        <div class="invite">
            <h1 class="title">共识矿场</h1>
            <p class="info">每邀请好友下载钱包并进入应用获得钻石奖励</p>
            <p class="info">每邀请一位注册获得200钻石奖励 (最高8000)</p>
            <div class="code">
                <p class="you-code">你的邀请码</p>
                <p class="invite-code">
                    <span>{{inviteCode}}</span>
                </p>
                <p class="copy">
                    <button v-clipboard:copy="inviteCode" v-clipboard:success="copySuccess"
                            v-clipboard:error="copyError">复制邀请码{{$store.$fun}}
                    </button>
                </p>
                <div class="data">
                    <div>
                        <p>已邀请</p>
                        <p>{{invitePeople}}人</p>
                    </div>
                    <div>
                        <p>获得奖励</p>
                        <p>{{inviteReward}}钻石</p>
                    </div>
                </div>
            </div>
            <div class="qr">
                <img src="../../assets/account.svg" class="img-qr">
                <p>扫码立即下载OX钱包</p>
            </div>
            <p class="about">
                本活动最终解释权归OX钱宝所有
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
                    message: "已复制到剪切板",
                    type: "success"
                });
            },
            copyError: function () {
                const _this = this;
                _this.$message({
                    showClose: true,
                    message: "复制失败",
                    type: "error"

                });
            },
        },
        created() {
            console.info(this);
        },
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
        font-size: 13px;
        margin: 0 0 5px 0;
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
