package org.tron.walletserver;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.*;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.UnfreezeAssetContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GrpcClient {

  private static final Logger logger = LoggerFactory.getLogger("GrpcClient");
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

//  public GrpcClient(String host, int port) {
//    channel = ManagedChannelBuilder.forAddress(host, port)
//        .usePlaintext(true)
//        .build();
//    blockingStub = WalletGrpc.newBlockingStub(channel);
//  }

  public GrpcClient(String fullnode, String soliditynode) {
    if (!StringUtils.isEmpty(fullnode)) {
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    }
    if (!StringUtils.isEmpty(soliditynode)) {
      channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
          .usePlaintext(true)
          .build();
      blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    }
  }

  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public Account queryAccount(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.getAccount(request);
    } else {
      return blockingStubFull.getAccount(request);
    }
  }

  public Transaction createTransaction(Contract.AccountUpdateContract contract) {
    return blockingStubFull.updateAccount(contract);
  }

  public Transaction createTransaction(Contract.TransferContract contract) {
    return blockingStubFull.createTransaction(contract);
  }

  public Transaction createTransaction(FreezeBalanceContract contract) {
    return blockingStubFull.freezeBalance(contract);
  }

  public Transaction createTransaction(WithdrawBalanceContract contract) {
    return blockingStubFull.withdrawBalance(contract);
  }

  public Transaction createTransaction(UnfreezeBalanceContract contract) {
    return blockingStubFull.unfreezeBalance(contract);
  }

  public Transaction createTransaction(UnfreezeAssetContract contract) {
    return blockingStubFull.unfreezeAsset(contract);
  }

  public Transaction createTransferAssetTransaction(Contract.TransferAssetContract contract) {
    return blockingStubFull.transferAsset(contract);
  }

  public Transaction createParticipateAssetIssueTransaction(
      Contract.ParticipateAssetIssueContract contract) {
    return blockingStubFull.participateAssetIssue(contract);
  }

  public Transaction createAssetIssue(Contract.AssetIssueContract contract) {
    return blockingStubFull.createAssetIssue(contract);
  }

  public Transaction voteWitnessAccount(Contract.VoteWitnessContract contract) {
    return blockingStubFull.voteWitnessAccount(contract);
  }

  public Transaction createWitness(Contract.WitnessCreateContract contract) {
    return blockingStubFull.createWitness(contract);
  }

  public Transaction updateWitness(Contract.WitnessUpdateContract contract) {
    return blockingStubFull.updateWitness(contract);
  }

  public boolean broadcastTransaction(Transaction signaturedTransaction) {
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(signaturedTransaction);
    while (response.getResult() == false && response.getCode() == response_code.SERVER_BUSY
        && i > 0) {
      i--;
      response = blockingStubFull.broadcastTransaction(signaturedTransaction);
      logger.info("Code = " + response.getCode());
      logger.info("Message = " + response.getMessage().toStringUtf8());
      logger.info("i = " + i);
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return response.getResult();
  }

  public Block getBlock(long blockNum) {
    if (blockNum < 0) {
      if (blockingStubSolidity != null) {
        return blockingStubSolidity.getNowBlock(EmptyMessage.newBuilder().build());
      } else {
        return blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
      }
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.getBlockByNum(builder.build());
    } else {
      return blockingStubFull.getBlockByNum(builder.build());
    }
  }

  public Optional<AccountList> listAccounts() {
    AccountList accountList = blockingStubSolidity
        .listAccounts(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(accountList);
  }

  public Optional<WitnessList> listWitnesses() {
    WitnessList witnessList = blockingStubSolidity
        .listWitnesses(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(witnessList);
  }

  public Optional<AssetIssueList> getAssetIssueList() {
    AssetIssueList assetIssueList = blockingStubSolidity
        .getAssetIssueList(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(assetIssueList);

  }

  public Optional<NodeList> listNodes() {
    NodeList nodeList = blockingStubFull
        .listNodes(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(nodeList);
  }

  public Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    if (blockingStubSolidity != null) {
      AssetIssueList assetIssueList = blockingStubSolidity
          .getAssetIssueByAccount(request);
      return Optional.ofNullable(assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull
          .getAssetIssueByAccount(request);
      return Optional.ofNullable(assetIssueList);
    }
  }

  public AssetIssueContract getAssetIssueByName(String assetName) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.getAssetIssueByName(request);
    } else {
      return blockingStubFull.getAssetIssueByName(request);
    }
  }

  public NumberMessage getTotalTransaction() {
    return blockingStubSolidity.totalTransaction(EmptyMessage.newBuilder().build());
  }

  public Optional<AssetIssueList> getAssetIssueListByTimestamp(long time) {
    NumberMessage.Builder timeStamp = NumberMessage.newBuilder();
    timeStamp.setNum(time);
    AssetIssueList assetIssueList = blockingStubSolidity
        .getAssetIssueListByTimestamp(timeStamp.build());
    return Optional.ofNullable(assetIssueList);
  }

  public Optional<TransactionList> getTransactionsByTimestamp(long start, long end) {
    TimeMessage.Builder timeMessage = TimeMessage.newBuilder();
    timeMessage.setBeginInMilliseconds(start);
    timeMessage.setEndInMilliseconds(end);
    TransactionList transactionList = blockingStubSolidity
        .getTransactionsByTimestamp(timeMessage.build());
    return Optional.ofNullable(transactionList);
  }

  public Optional<TransactionList> getTransactionsFromThis(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    TransactionList transactionList = blockingStubSolidity.getTransactionsFromThis(request);
    return Optional.ofNullable(transactionList);
  }

  public Optional<TransactionList> getTransactionsToThis(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    TransactionList transactionList = blockingStubSolidity.getTransactionsToThis(request);
    return Optional.ofNullable(transactionList);
  }

  public Optional<Transaction> getTransactionById(String txID) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    if (blockingStubSolidity != null) {
      Transaction transaction = blockingStubSolidity.getTransactionById(request);
      return Optional.ofNullable(transaction);
    } else {
      Transaction transaction = blockingStubFull.getTransactionById(request);
      return Optional.ofNullable(transaction);
    }
  }

  public Optional<Block> getBlockById(String blockID) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(blockID));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Block block = blockingStubFull.getBlockById(request);
    return Optional.ofNullable(block);
  }

  public Optional<BlockList> getBlockByLimitNext(long start, long end) {
    BlockLimit.Builder builder = BlockLimit.newBuilder();
    builder.setStartNum(start);
    builder.setEndNum(end);
    BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    return Optional.ofNullable(blockList);
  }

  public Optional<BlockList> getBlockByLatestNum(long num) {
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
    BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    return Optional.ofNullable(blockList);
  }
}
