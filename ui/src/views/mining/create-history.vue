<template>
    <div class="create-history">
        <p @click="$router.back()" class="mining-back">&lt;&lt;返回上一页</p>
        <div class="history">
            <div class="history-content" v-for="history in historyList" @click="details(history)">
                <p>
                    <img src="../../assets/kuangchisouyi.png">
                    <span>创建时间</span>:
                    <span>{{history.createTime}}</span>
                </p>
                <p>
                    <img src="../../assets/kuagnchifhenpei.png">
                    <span>挖矿时长</span>:
                    <span>{{history.miningTime}}块矿产(约12小时)</span>
                </p>
                <p>
                    <img src="../../assets/kuangchishenyu.png">
                    <span>矿池收益</span>:
                    <span>{{history.earnings}}砖石</span>
                </p>
            </div>
        </div>
        <div v-if="isDetails">
            <div class="history-details">
                <div class="details">
                    <h1>矿池详情</h1>
                    <p>
                        <span>创建时间</span>:
                        <span>{{historyDetails.createTime}}</span>
                    </p>
                    <p>
                        <span>结束时间</span>:
                        <span>{{historyDetails.endTime}}</span>
                    </p>
                    <p>
                        <span>挖矿时长</span>:
                        <span>{{historyDetails.miningTime}}块矿产(约12小时)</span>
                    </p>
                    <p>
                        <span>矿池收益</span>:
                        <span>{{historyDetails.earnings}}钻石</span>
                    </p>
                    <p>
                        <span>奖励分配</span>:
                        <span>{{historyDetails.rewards}}%</span>
                    </p>
                    <p>
                        <span>加入人数</span>:
                        <span>{{historyDetails.numberPeople}}人</span>
                    </p>
                    <p>
                        <span>矿池砖石</span>:
                        <span>{{historyDetails.maximum}}(最大值)</span>
                    </p>
                </div>
                <div class="close" @click="details(historyDetails)">关闭</div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "create-history",
        data() {
            return {
                historyList: [
                    {
                        createTime: "2018-10-10 12:00",
                        endTime: "2018-10-10 12:00",
                        miningTime: 800,
                        earnings: 1000,
                        rewards: 90,
                        numberPeople: 6,
                        maximum: 300000,
                    },
                    {
                        createTime: "2018-10-13 12:00",
                        endTime: "2018-10-15 12:00",
                        miningTime: 800,
                        earnings: 1000,
                        rewards: 90,
                        numberPeople: 6,
                        maximum: 300000,
                    },
                    {
                        createTime: "2018-10-16 12:00",
                        endTime: "2018-10-18 12:00",
                        miningTime: 800,
                        earnings: 1000,
                        rewards: 90,
                        numberPeople: 6,
                        maximum: 300000,
                    },
                    {
                        createTime: "2018-10-19 12:00",
                        endTime: "2018-10-21 12:00",
                        miningTime: 800,
                        earnings: 1000,
                        rewards: 90,
                        numberPeople: 6,
                        maximum: 300000,
                    },
                ],
                historyDetails: '',
                isDetails: false,
            }
        },
        methods: {
            details(val) {
                // if (this.isDetails) {
                //     this.$store.state.mask = false;
                // } else {
                //     this.$store.state.mask = true;
                // }
                this.$store.state.mask = !this.isDetails;
                this.isDetails = !this.isDetails;
                this.historyDetails = val;
            },
        }
    }
</script>

<style scoped>
    .create-history .history {
        padding: 15px;
    }

    .history .history-content {
        background: #fff;
        margin: 15px 0;
        border-radius: 6px;
        padding: 10px 20px;
    }

    .history .history-content p {
        padding: 5px 0;
    }

    .history-content p img {
        position: relative;
        top: 1px;
        margin-right: 5px;
    }

    .history-details {
        position: fixed;
        top: calc(50% - 150px);
        width: calc(100% - 20px);
        left: 10px;
        background: #fff;
        z-index: 9999;
        border-radius: 8px;
    }

    .history-details .details {
        padding: 15px 30px 10px;
    }

    .history-details .details h1 {
        padding: 0 0 20px;
        font-size: 14px;
        color: #333;
        font-weight: bold;
        text-align: center;
    }

    .history-details .details p {
        margin: 0 0 10px 0;
        font-size: 12px;
    }

    .history-details .details p span:first-child {
        font-weight: bold;
    }

    .history-details .close {
        width: 100%;
        text-align: center;
        padding: 10px 0;
        background: #513ac8;
        color: #fff;
        font-size: 15px;
        border-bottom-left-radius: 6px;
        border-bottom-right-radius: 6px;
    }

</style>
