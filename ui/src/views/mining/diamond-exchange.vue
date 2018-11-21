<template>
    <div class="diamond-exchange">
        <p @click="$router.back()" class="mining-back">&lt;&lt;返回上一页</p>
        <div class="exchange">
            <div class="exchange-header">
                <h1>砖石兑换</h1>
                <p>砖石限时开发兑换中,数量有限先到先得</p>
            </div>
            <div class="exchange-list" v-for="exchange in exchangeList">
                <p>
                    <img :src="exchange.img" class="exchange-img">
                    <span class="title">
                        <span class="strong">{{exchange.name}}</span>
                        <span>剩余 : {{exchange.num}}</span>
                    </span>
                </p>
                <p>说明:{{exchange.info}}</p>
                <button>暂未开放</button>
            </div>
            <div class="info">
                更多兑换即将开启!
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "diamond-exchange",
        data() {
            return {
                exchangeList: [
                    {
                        img: "http://localhost:4000/76894d35b252344138a2de2a1927d9ca.svg",
                        name: "1000豆匣SS",
                        num: 0,
                        info: "兑换成功后请联系官方管理员进行领取",
                        state: "",
                    },
                    {
                        img: "http://localhost:4000/76894d35b252344138a2de2a1927d9ca.svg",
                        name: "200元优惠劵",
                        num: 0,
                        info: "用于购买豆匣矿机时抵扣响应价格",
                        state: "",
                    },
                    {
                        img: "http://localhost:4000/76894d35b252344138a2de2a1927d9ca.svg",
                        name: "500元优惠劵",
                        num: 0,
                        info: "用于购买豆匣矿机时抵扣响应价格",
                        state: "",
                    },
                    {
                        img: "http://localhost:4000/76894d35b252344138a2de2a1927d9ca.svg",
                        name: "800元优惠劵",
                        num: 0,
                        info: "用于购买豆匣矿机时抵扣响应价格",
                        state: "",
                    },
                ],
            }
        },
    }
</script>

<style scoped>
    .diamond-exchange {
        position: fixed;
        left: 0;
        right: 0;
        top: 0;
        bottom: 0;
        background: #513acB;
        color: #fff;
        overflow: auto;
    }

    .diamond-exchange .mining-back {
        color: #fff;
    }

    .diamond-exchange .exchange {
        padding: 30px 15px;
    }

    .exchange .exchange-header h1 {
        font-size: 28px;
        font-weight: bold;
        padding: 20px 0 10px;
    }

    .exchange .exchange-header p {
        padding: 0 0 20px;
    }

    .exchange .exchange-list {
        width: 100%;
        height: 70px;
        border-radius: 4px;
        background: #fff;
        color: #333;
        padding: 13px 10px;
        position: relative;
        margin: 0 0 10px;
    }

    .exchange .exchange-list .exchange-img {
        position: relative;
        top: 0;
        left: 0;
        width: 20px;
        height: 20px;
    }

    .exchange .exchange-list p {
        font-size: 11px;
        color: #999;
    }

    .exchange .exchange-list button {
        position: absolute;
        top: 16px;
        right: 10px;
        outline: none;
        border: none;
        border-radius: 4px;
        background: #dbe2e8;
        height: 40px;
        width: 100px;
        color: #fff;
        font-size: 14px;
        font-weight: bold;
    }

    .exchange .exchange-list .title {
        display: inline-block;
        position: relative;
        top: -4px;
        padding: 5px 0 5px;
    }

    .exchange-list .title .strong {
        font-size: 18px;
        padding-right: 10px;
        color: #666;
    }

    .exchange .exchange-list + .info {
        padding: 20px 0;
        text-align: center;
        font-size: 14px;
        border-radius: 4px;
        background: #fff;
        margin: 0 0 20px;
        color: #333;
    }
</style>
