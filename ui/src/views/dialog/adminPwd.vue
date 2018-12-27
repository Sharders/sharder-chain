<template>
    <div class="modal" id="set_admin_password_modal" v-show="adminPasswordDialog">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button class="close" @click="closeDialog">X</button>
                    <h4 class="modal-title">{{$t('password_modal.title')}}</h4>
                </div>
                <div class="modal-body modal-peer">
                    <p>{{$t('password_modal.admin_password')}}</p>
                    <input v-model="adminPassword" type="password"/>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn" @click="returnValue()">{{$t('password_modal.open')}}</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "adminPwd",
        props:{
            openDialog:Boolean,
        },
        data(){
            return{
                adminPasswordDialog:false,
                adminPassword:'',
            }
        },
        methods:{
            returnValue(){
                const _this = this;
                _this.$store.state.mask = false;
                _this.$emit('getPwd',_this.adminPassword);
            },
            closeDialog(){
                const _this = this;
                _this.$store.state.mask = false;
                _this.$emit('isClose',false);
            }
        },
        watch:{
            openDialog: function (val) {
                const _this = this;
                _this.adminPasswordDialog = val;
                if(val)
                {
                    _this.$store.state.mask = true;
                }
            }
        }

    }

</script>

<style scoped>

</style>
