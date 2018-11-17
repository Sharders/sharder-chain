<template>
    <div class="pool-attribute">
        <p @click="$router.back()" class="pool-back">&lt;&lt;返回矿池{{$route.params}}</p>
        <div class="pool-content">
            <div class="attribute-info">
                <img src="" class="">
                <div class="attribute-text">
                    <span class="pool-serial-number">矿池编号:001 | 挖矿几率 : 90%</span>
                    <span class="pool-attribute-info">矿池属性</span>
                </div>
                <div class="earnings">收益+2000SS</div>
            </div>
            <div class="my-info">
                <h1>
                    <img src="" class="head-portrait">
                    <span>我的信息</span>
                </h1>
                <div class="my-attribute">
                    <el-row :gutter="20">
                        <el-col :span="6">
                            <p>加入时间</p>
                            <p>1018-06-13 12:00</p>
                        </el-col>
                    </el-row>
                </div>
            </div>
            <div class="attribute-btn">
                <button class="join">投入SS</button>
                <button class="exit">退出矿池</button>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "attribute"
    }
</script>

<style scoped>

</style>
