package org.conch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Consignor implements Serializable {
    private static final long serialVersionUID = 1214235652377897690L;
    private final long id;
    private final List<JoinTransaction> transactions = new ArrayList<>();

    public Consignor(long id,long transactionId,int startBlockNo,int endBlockNo,long amount){
        JoinTransaction joinTransaction = new JoinTransaction(transactionId,startBlockNo,endBlockNo,amount);
        this.id = id;
        this.transactions.add(joinTransaction);
    }

    public void addTransaction(long transactionId,int startBlockNo,int endBlockNo,long amount){
        JoinTransaction joinTransaction = new JoinTransaction(transactionId,startBlockNo,endBlockNo,amount);
        this.transactions.add(joinTransaction);
    }

    public boolean removeTransaction(long txId){
        JoinTransaction joinTransaction = new JoinTransaction(txId,0,0,0);
        if(transactions.contains(joinTransaction)){
            transactions.remove(joinTransaction);
        }
        if(transactions.size() == 0){
            return true;
        }
        return false;
    }

    public long getTransactionAmount(long txId){
        JoinTransaction joinTransaction = new JoinTransaction(txId,0,0,0);
        if(transactions.contains(joinTransaction)){
            int index = transactions.indexOf(joinTransaction);
            return transactions.get(index).getAmount();
        }
        return -1;
    }

    public long getTransactionEndNo(long txId){
        JoinTransaction joinTransaction = new JoinTransaction(txId,0,0,0);
        if(transactions.contains(joinTransaction)){
            int index = transactions.indexOf(joinTransaction);
            return transactions.get(index).getEndBlockNo();
        }
        return -1;
    }

    public boolean hasTransaction(long txId){
        JoinTransaction joinTransaction = new JoinTransaction(txId,0,0,0);
        if(transactions.contains(joinTransaction)){
            return true;
        }
        return false;
    }

    public long validateHeight(int height){
        long amount = 0;
        List<JoinTransaction> timeout = new ArrayList<>();
        for(JoinTransaction joinTransaction : transactions){
            if(joinTransaction.getEndBlockNo() == height){
                timeout.add(joinTransaction);
                amount += joinTransaction.getAmount();
            }
        }
        transactions.remove(timeout);
        return amount;
    }

    public long getAmount(){
        long amount = 0;
        for(JoinTransaction joinTransaction : transactions){
            amount += joinTransaction.getAmount();
        }
        return amount;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Consignor consignor = (Consignor) o;

        return id == consignor.id;
    }

    public class JoinTransaction implements Serializable{
        private static final long serialVersionUID = 989675312314345121L;
        private final long transactionId;
        private final int startBlockNo;
        private final int endBlockNo;
        private final long amount;

        public JoinTransaction(long transactionId,int startBlockNo,int endBlockNo,long amount){
            this.transactionId = transactionId;
            this.startBlockNo = startBlockNo;
            this.endBlockNo = endBlockNo;
            this.amount = amount;
        }

        public long getTransactionId() {
            return transactionId;
        }

        public int getEndBlockNo() {
            return endBlockNo;
        }

        public long getAmount() {
            return amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JoinTransaction joinTransaction = (JoinTransaction) o;

            return transactionId == joinTransaction.getTransactionId();
        }
    }
}
