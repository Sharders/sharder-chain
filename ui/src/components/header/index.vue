<template>
<header>
    <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" :router=isRouter>
        <el-menu-item index="/account">账户</el-menu-item>
        <el-menu-item index="/mining">网络</el-menu-item>
        <el-menu-item index="/network">矿池</el-menu-item>
    </el-menu>
    </header>
</template>

<script>
export default {
  name: 'Header',
  props: ['openSidebar', 'title'],
    data () {
        return {
            activeIndex:'1',
            isRouter:true,
        }
    },
    methods:{

    }
}
</script>

