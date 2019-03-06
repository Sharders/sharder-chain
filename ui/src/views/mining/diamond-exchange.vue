<template>
    <div class="diamond-exchange">
        <p @click="$router.back()" class="mining-back">&lt;&lt;{{$t('mining.attribute.return_previous')}}</p>
        <ExchangeReward></ExchangeReward>
    </div>
</template>

<script>
    import ExchangeReward from "./exchange-reward";

    export default {
        name: "diamond-exchange",
        components: {
            "ExchangeReward": ExchangeReward
        },
        data() {
            return {}
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
        padding: 30px 15px 0;
    }

    .diamond-exchange .mining-back {
        color: #fff;
    }
</style>
