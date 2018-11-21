<template>
    <div class="free-collar-drill">
        <p @click="$router.back()" class="mining-back">&lt;&lt;返回上一页</p>
        <div class="free-collar">
            <div class="free-header">
                <h1>免费领钻</h1>
                <p>免费领取砖石,走向致富之路</p>
            </div>
            <div class="free-list" v-for="free in freeList">
                <p>
                    <span>{{free.name}}</span>
                    <span>+{{free.num}}</span>
                    <span>砖石</span>
                </p>
                <span class="state-info" v-if="free.state !== 'IN'">
                    <span>{{stateInfo(free.state)}}</span>
                </span>
                <button v-if="free.state === 'IN'">立即领取</button>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "free-collar-drill",
        data() {
            return {
                freeList: [
                    {
                        name: "每日登陆",
                        num: 100,
                        state: "GET",
                    },
                    {
                        name: "注册赠送",
                        num: 100,
                        state: "IN",
                    },
                    {
                        name: "绑定手机",
                        num: 100,
                        state: "IN",
                    },
                    {
                        name: "绑定邮箱",
                        num: 100,
                        state: "IN",
                    },
                ],
            }
        }, methods: {
            stateInfo(val) {
                if (val === "GET") {
                    return "已领取";
                }
            }
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
        background: url("../../assets/state.png") no-repeat;
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
