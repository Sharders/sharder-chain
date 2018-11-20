<template>
    <div class="rule-description">
        <p @click="$router.back()" class="mining-back">&lt;&lt;返回矿池</p>
        <div class="description">
            <h1 class="description-title">系统规则</h1>
            <p class="description-info">豆匣矿场规则说明,不看错过一个亿</p>
            <div class="info">
                <h3>豆匣矿场</h3>
                <p>
                    "豆匣矿场"是豆匣协议基于豆匣链开发的一款挖矿软件DAPP应用.
                    以区块链技术为核心,挖矿为玩法.所有数据同步区块链网络,保证数据可靠及准确性.
                </p>
            </div>
            <div class="info">
                <h3>挖矿</h3>
                <p>"豆匣矿场"的核心玩法,以挖矿为单位进行挖矿行为.
                    挖矿成功获得砖石奖励,未成功挖矿将在下一个矿产继续争夺挖矿权.根据钻石数量决定挖矿的成功几率.
                </p>
            </div>
            <div class="info">
                <h3>矿池</h3>
                <p>
                    拥有豆匣矿机的用户可以创建矿池,用户可投入钻石参与矿池挖矿.收益将根据投入的数量进行分配,全网矿池数量固定为51个.
                    矿池的生命周期固定为48小时,打到生命周期后矿池将解散,投入及奖励的钻石奖返还给用户.
                </p>
            </div>
            <div class="info">
                <h3>收益分配</h3>
                <p>
                    创建矿池时可以自定义收益分配比例,挖矿成功后将根据设定的比例进行奖励分配,获得奖励也与投入矿池的数量成正比
                </p>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "rule-description"
    }
</script>

<style scoped>
    .rule-description .description {
        padding: 15px;
        background: #fff;
    }

    .description .description-title {
        font-size: 27px;
        font-weight: bold;
        padding: 30px 0 0 0;
        color: #513ac8;
    }

    .description .description-info {
        color: #513ac8;
        padding: 10px 0 30px
    }

    .rule-description .description .info {
        padding: 15px;
        color: #fff;
        background: #513ac8;
        border-radius: 4px;
        margin: 0 0 15px 0;
    }

    .description .info h3 {
        font-size: 17px;
        font-weight: bold;
        padding: 0 0 10px 0;
    }

</style>
