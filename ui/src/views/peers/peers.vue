<template>
    <div class="row page " id="peers_page">
        <div class="container ">
            <div class="block-network hrefbtn">
                <a class="block-title" data-page="new_block">&lt;&lt;返回网络</a>
                <div>
                    <div class="block-blue">
                        <p>在线节点数</p>
                        <p><span id="block-peerNum"></span>个</p>
                    </div>
                    <div class="block-blue">
                        <p>HUB运行数</p>
                        <p><span id="block-hubOnline"></span>个</p>
                    </div>
                    <div class="block-blue">
                        <p>活跃节点数</p>
                        <p><span id="block-activePeer"></span>个</p>
                    </div>
                </div>
            </div>
            <div class="block-map s-section">

            </div>
            <div class="block-list">
                <p>
                    <img src="../../assets/peerlist.png"/>
                    <span class="block-title">节点列表</span>
                </p>
                <div>
                    <div class="list-content data-container table-responsive table-responsive data-loading">
                        <table class="table table-striped table-hover"  id="peers_table">
                            <thead>
                            <tr>
                                <th>节点地址</th>
                                <th>已下载</th>
                                <th>已上传</th>
                                <th>应用程序</th>
                                <th>平台</th>
                                <th>服务</th>
                                <th>操作</th>
                            </tr>
                            </thead>
                            <tbody>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

</template>

<script>
    export default {
        name: ""
    }
</script>
