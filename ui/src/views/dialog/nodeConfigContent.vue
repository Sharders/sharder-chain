<template>
    <div class="nodeConfigContent">
        <el-row>
            <el-col :xl="24" :lg="24" :md="24" :sm="24" :xs="24">
                <el-card shadow="hover">
                    <div class="address">
                        <span>IP : </span>
                        <span>{{ pocInfo.ip }}</span>
                        <span>Port : </span>
                        <span>{{ pocInfo.port }}</span>
                    </div>
                    <div>
                        <span>CPU核心数 : </span>
                        <span>{{ pocInfo.core }}</span>
                        <span>平均兆赫兹 : </span>
                        <span>{{ pocInfo.averageMHz }} MHz</span>
                        <span>内存大小 : </span>
                        <span>{{ pocInfo.memoryTotal }} GB</span>
                        <span>硬盘大小 : </span>
                        <span>{{ pocInfo.hardDiskSize }} GB</span>
                        <span>是否有公网IP : </span>
                        <span>{{ pocInfo.hadPublicIp }}</span>
                        <span>公网带宽: </span>
                        <span>{{ pocInfo.bandWidth }} Mbps</span>
                        <span>交易处理性能 : </span>
                        <span>{{ pocInfo.tradePerformance }}</span>
                        <span>开启服务 : </span>
                        <el-tag v-for="openServiceCode in pocInfo.openServices">
                            {{ openServicesMapping(openServiceCode) }}
                        </el-tag>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script>
    export default {
        name: "nodeConfigContent",
        props: {
            pocInfo:{}
        },
        methods: {
            openServicesMapping(openServiceCode) {
                switch (openServiceCode) {
                    case 1:
                        return "HALLMARK";
                    case 2:
                        return "PRUNABLE";
                    case 4:
                        return "API";
                    case 8:
                        return "API_SSL";
                    case 16:
                        return "CORS";
                    case 32:
                        return "BAPI";
                    case 64:
                        return "STORAGE";
                    case 128:
                        return "MINER";
                    case 256:
                        return "NATER";
                    case 512:
                        return "PROVER";
                }
            },
        }
    }
</script>

<style scoped>
    .nodeConfigContent .el-card div {
        padding: 10px;
    }

    .nodeConfigContent .el-card div span {
        width: 150px;
    }
</style>
