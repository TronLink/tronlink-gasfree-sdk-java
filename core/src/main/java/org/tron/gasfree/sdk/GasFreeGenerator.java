package org.tron.gasfree.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.tron.common.bip32.Numeric;
import org.tron.common.crypto.FunctionEncoder;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.StructuredDataEncoder;
import org.tron.common.crypto.datatypes.Address;
import org.tron.common.crypto.datatypes.DynamicBytes;
import org.tron.common.crypto.datatypes.Function;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LogUtils;
import org.tron.common.utils.abi.DataWord;
import org.tron.config.Parameter;
import org.tron.walletserver.AddressUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

public class GasFreeGenerator {

    public static String generateGasFreeAddress(String userAddress, String chainId) {
        if (Constant.NileNetCode.equals(chainId)) {
            return generateGasFreeAddress(userAddress, Constant.beaconAddressNile, Constant.GasFreeControllerAddressNile, Constant.creationCodeStr);
        } else if (Constant.MainNetCode.equals(chainId)) {
            return generateGasFreeAddress(userAddress, Constant.beaconAddressRelease, Constant.GasFreeControllerAddressShasta, Constant.creationCodeStr);
        } else if (Constant.ShastaNetCode.equals(chainId)) {
            return generateGasFreeAddress(userAddress, Constant.beaconAddressShasta, Constant.GasFreeControllerAddressShasta, Constant.creationCodeStr);
        } else {
            return generateGasFreeAddress(userAddress, Constant.beaconAddressRelease, Constant.GasFreeControllerAddressRelease, Constant.creationCodeStr);
        }
    }

    public static String generateGasFreeAddress(String user, String beanconAddress, String gasFreeControlAddress, String creationCodeStr) {
        try {
            byte[] salt = byte32Address(user);
            Function function = new Function(
                    "initialize",
                    Arrays.asList(new Address(user)),
                    Collections.emptyList());
            String initializeFun = FunctionEncoder.encode(function);

            String encodeCall = FunctionEncoder.encodeConstructor(
                    Arrays.asList(new Address(AddressUtil.replace41Address(beanconAddress)),
                            new DynamicBytes(ByteArray.fromHexString(initializeFun))));
            byte[] creationCode = ByteArray.fromHexString(creationCodeStr);
            // encodePacked
            // cannot use FunctionEncoder.encodeConstructorPacked, because DynamicTypes packed function changed
            byte[] bytecodeHash = Hash.sha3(ByteUtils.concatenate(creationCode, ByteArray.fromHexString(encodeCall)));

            // create2
            byte[] bytes = ByteUtils.concatenate(ByteArray.fromHexString(Parameter.CommonConstant.ADD_PRE_FIX_STRING), ByteArray.fromHexString(AddressUtil.replace41Address(gasFreeControlAddress)));
            byte[] bytes1 = ByteUtils.concatenate(bytes, salt);
            byte[] bytes2 = ByteUtils.concatenate(bytes1, bytecodeHash);
            byte[] bytes3 = Hash.sha3omit12(bytes2);
            String gasFreeAddress = AddressUtil.encode58Check(bytes3);
            return gasFreeAddress;
        } catch (Exception e) {
            LogUtils.e(e);
        }
        return "";
    }

    public static String permitTransferMessageHash(String eip712Message) {
        try {
            StructuredDataEncoder dataEncoder = new StructuredDataEncoder(eip712Message);
            byte[] bytes = dataEncoder.hashDomain();
            String domainStr = Numeric.toHexString(bytes);

            byte[] hashMessage = dataEncoder.hashMessage();
            String messageStr = Numeric.toHexString(hashMessage);
            Gson gson = new Gson();
            JsonObject data
                    = gson.fromJson(eip712Message, JsonObject.class);
            String message = data.get("message").toString();
            GasFreeMessage object = gson.fromJson(message, GasFreeMessage.class);
            if(object == null){
                throw new Exception("Invalid input message");
            }
            if(!AddressUtil.isAddressValid(object.getToken())){
                throw new AddressException("Invalid message.token: ${"+object.getToken() + "}, should be a valid Tron address");
            }
            if(!AddressUtil.isAddressValid(object.getUser())){
                throw new AddressException("Invalid message.user: ${"+object.getUser() + "}, should be a valid Tron address");
            }
            if(!AddressUtil.isAddressValid(object.getReceiver())){
                throw new AddressException("Invalid message.receiver: ${"+object.getReceiver() + "}, should be a valid Tron address");
            }
            if(!AddressUtil.isAddressValid(object.getServiceProvider())){
                throw new AddressException("Invalid message.provider: ${"+object.getServiceProvider() + "}, should be a valid Tron address");
            }
            byte[] hashStructuredData = dataEncoder.hashStructuredData();
            String hash = Numeric.toHexString(hashStructuredData);
            return hash;
        } catch (Exception e) {
            LogUtils.e(e);
        }
        return "";
    }

    public static boolean gasFreeParamCheck() {
        return false;
    }

    private static byte[] byte32Address(String address) {
        String value = AddressUtil.replace41Address(address);
        byte[] addressBytes = ByteArray.fromHexString(value);
        if (addressBytes == null) {
            return null;
        }
        return new DataWord(addressBytes).getData();
    }
}

