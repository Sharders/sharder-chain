<template>
    <div class="exchange">
        <div class="exchange-header">
            <h1>{{$t('mining.diamond_exchange.diamond_exchange_title')}}</h1>
            <p>{{$t('mining.diamond_exchange.diamond_exchange_subtitle')}}</p>
        </div>
        <div class="exchange-list" :class="(index+1)%3 === 0 ? ' right' :''" v-for="(exchange,index) in exchangeList">
            <p>
                <img :src="exchange.img" class="exchange-img">
                <span class="title">
                        <span class="strong">{{exchange.name}}</span>
                        <span>{{$t('mining.diamond_exchange.remaining')}}{{exchange.num}}</span>
                    </span>
            </p>
            <p>{{$t('mining.diamond_exchange.description')}}{{exchange.info}}</p>
            <button>{{$t('mining.diamond_exchange.not_open')}}</button>
        </div>
        <div class="exchange-list info">
            {{$t('mining.diamond_exchange.not_open_tip')}}
        </div>
    </div>
</template>

<script>
    export default {
        name: "exchange-reward",
        data() {
            return {
                exchangeList: [
                    {
                        img: "http://localhost:4000/76894d35b252344138a2de2a1927d9ca.svg",
                        name: "100豆匣SS",
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
                        name: "300元优惠劵",
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
                        name: "800豆匣SS",
                        num: 0,
                        info: "兑换成功后请联系官方管理员进行领取",
                        state: "",
                    },
                    {
                        img: "http://localhost:4000/76894d35b252344138a2de2a1927d9ca.svg",
                        name: "1000元优惠劵",
                        num: 0,
                        info: "用于购买豆匣矿机时抵扣响应价格",
                        state: "",
                    },
                    {
                        img: "http://localhost:4000/76894d35b252344138a2de2a1927d9ca.svg",
                        name: "1500元优惠劵",
                        num: 0,
                        info: "用于购买豆匣矿机时抵扣响应价格",
                        state: "",
                    },
                    {
                        img: "http://localhost:4000/76894d35b252344138a2de2a1927d9ca.svg",
                        name: "1800元优惠劵",
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
    .exchange .exchange-header h1 {
        font-size: 28px;
        font-weight: bold;
        padding: 20px 0 10px;
    }

    .exchange .exchange-header p {
        padding: 0 0 20px;
    }

    .exchange .exchange-list {
        float: left;
        width: 360px;
        display: inline-block;
        height: 70px;
        border-radius: 4px;
        background: #fff;
        color: #333;
        padding: 13px 10px;
        position: relative;
        margin: 0 60px 20px 0;
    }

    .exchange .exchange-list.right {
        margin: 0 0 20px;
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

    .exchange .exchange-list.info {
        padding: 24px 0;
        text-align: center;
        margin: 0 0 20px;
    }

    @media (max-width: 640px) {
        .exchange .exchange-list {
            max-width: 100%;
        }
    }
</style>
