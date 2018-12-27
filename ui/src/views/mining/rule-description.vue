<template>
    <div class="rule-description">
        <p @click="$router.back()" class="mining-back">&lt;&lt;{{$t('mining.attribute.return_previous')}}</p>
        <div class="description">
            <h1 class="description-title">{{$t('mining.rule_description.rule_description_title')}}</h1>
            <p class="description-info">{{$t('mining.rule_description.rule_description_subtitle')}}</p>
            <div class="info">
                <h3>{{$t('mining.rule_description.sharder_mine')}}</h3>
                <p>
                    {{$t('mining.rule_description.sharder_mine_tip1')}}
                    {{$t('mining.rule_description.sharder_mine_tip2')}}.
                </p>
            </div>
            <div class="info">
                <h3>{{$t('mining.rule_description.mining')}}</h3>
                <p>{{$t('mining.rule_description.mining_tip1')}}
                    {{$t('mining.rule_description.mining_tip2')}}
                </p>
            </div>
            <div class="info">
                <h3>{{$t('mining.rule_description.pool')}}</h3>
                <p>
                    {{$t('mining.rule_description.pool_tip1')}}
                    {{$t('mining.rule_description.pool_tip2')}}
                </p>
            </div>
            <div class="info">
                <h3>{{$t('mining.rule_description.income_distribution')}}</h3>
                <p>
                    {{$t('mining.rule_description.income_distribution_tip1')}}
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
