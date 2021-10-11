<template>
    <div class="create-history">
        <p @click="$router.back()" class="mining-back">{{$t('mining.attribute.return_previous')}}</p>
        <div class="history">
            <div class="history-content" v-for="history in historyList" @click="details(history)">
                <p>
                    <img src="../../assets/img/kuangchisouyi.png" v-if="$global.projectName === 'mw'">
                    <img src="../../assets/img/sharder/kuangchisouyi.png" v-else-if="$global.projectName === 'sharder'">
                    <span>{{$t('mining.create_history.create_time')}}</span>:
                    <span>{{history.createTime}}</span>
                </p>
                <p>
                    <img src="../../assets/img/kuagnchifhenpei.png" v-if="$global.projectName === 'mw'">
                    <img src="../../assets/img/sharder/kuagnchifhenpei.png" v-else-if="$global.projectName === 'sharder'">
                    <span>{{$t('mining.create_history.mining_time')}}</span>:
                    <span>{{history.miningTime}}{{$t('mining.create_history.mining_time_tip')}}</span>
                </p>
                <p>
                    <img src="../../assets/img/kuangchishenyu.png" v-if="$global.projectName === 'mw'">
                    <img src="../../assets/img/sharder/kuangchishenyu.png" v-else-if="$global.projectName === 'sharder'">
                    <span>{{$t('mining.create_history.mining_income')}}</span>:
                    <span>{{history.earnings}}{{$t('mining.create_history.diamond')}}</span>
                </p>
            </div>
        </div>
        <div v-if="isDetails">
            <div class="history-details">
                <div class="details">
                    <h1>{{$t('mining.attribute.pool_details')}}</h1>
                    <p>
                        <span>{{$t('mining.attribute.create_time')}}</span>:
                        <span>{{historyDetails.createTime}}</span>
                    </p>
                    <p>
                        <span>{{$t('mining.create_history.end_time')}}</span>:
                        <span>{{historyDetails.endTime}}</span>
                    </p>
                    <p>
                        <span>{{$t('mining.create_history.mining_time')}}</span>:
                        <span>{{historyDetails.miningTime}}{{$t('mining.create_history.mining_time_tip')}}</span>
                    </p>
                    <p>
                        <span>{{$t('mining.create_history.mining_income')}}</span>:
                        <span>{{historyDetails.earnings}}{{$t('mining.create_history.diamond')}}</span>
                    </p>
                    <p>
                        <span>{{$t('mining.create_history.reward_distribution')}}</span>:
                        <span>{{historyDetails.rewards}}%</span>
                    </p>
                    <p>
                        <span>{{$t('mining.create_history.join_people')}}</span>:
                        <span>{{historyDetails.numberPeople}}</span>
                    </p>
                    <p>
                        <span>{{$t('mining.create_history.pool_diamond')}}</span>:
                        <span>{{historyDetails.maximum}}{{$t('mining.create_history.max')}}</span>
                    </p>
                </div>
                <div class="close" @click="details(historyDetails)">{{$t('mining.create_history.close')}}</div>
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

<style scoped type="text/scss" lang="scss">
@import '../../styles/css/vars.scss';
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
        color: #555;
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
        background: $primary_color;
        color: #fff;
        font-size: 15px;
        border-bottom-left-radius: 6px;
        border-bottom-right-radius: 6px;
    }

</style>
