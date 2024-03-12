/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v5v1cpv.api;

public class CThostFtdcInputCombActionField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcInputCombActionField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcInputCombActionField obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        jctpv6v5v1cpx64apiJNI.delete_CThostFtdcInputCombActionField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setBrokerID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_BrokerID_get(swigCPtr, this);
  }

  public void setInvestorID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_InvestorID_set(swigCPtr, this, value);
  }

  public String getInvestorID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_InvestorID_get(swigCPtr, this);
  }

  public void setReserve1(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_reserve1_set(swigCPtr, this, value);
  }

  public String getReserve1() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_reserve1_get(swigCPtr, this);
  }

  public void setCombActionRef(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_CombActionRef_set(swigCPtr, this, value);
  }

  public String getCombActionRef() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_CombActionRef_get(swigCPtr, this);
  }

  public void setUserID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_UserID_set(swigCPtr, this, value);
  }

  public String getUserID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_UserID_get(swigCPtr, this);
  }

  public void setDirection(char value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_Direction_set(swigCPtr, this, value);
  }

  public char getDirection() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_Direction_get(swigCPtr, this);
  }

  public void setVolume(int value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_Volume_set(swigCPtr, this, value);
  }

  public int getVolume() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_Volume_get(swigCPtr, this);
  }

  public void setCombDirection(char value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_CombDirection_set(swigCPtr, this, value);
  }

  public char getCombDirection() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_CombDirection_get(swigCPtr, this);
  }

  public void setHedgeFlag(char value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_HedgeFlag_set(swigCPtr, this, value);
  }

  public char getHedgeFlag() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_HedgeFlag_get(swigCPtr, this);
  }

  public void setExchangeID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_ExchangeID_set(swigCPtr, this, value);
  }

  public String getExchangeID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_ExchangeID_get(swigCPtr, this);
  }

  public void setReserve2(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_reserve2_set(swigCPtr, this, value);
  }

  public String getReserve2() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_reserve2_get(swigCPtr, this);
  }

  public void setMacAddress(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_MacAddress_set(swigCPtr, this, value);
  }

  public String getMacAddress() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_MacAddress_get(swigCPtr, this);
  }

  public void setInvestUnitID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_InvestUnitID_set(swigCPtr, this, value);
  }

  public String getInvestUnitID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_InvestUnitID_get(swigCPtr, this);
  }

  public void setFrontID(int value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_FrontID_set(swigCPtr, this, value);
  }

  public int getFrontID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_FrontID_get(swigCPtr, this);
  }

  public void setSessionID(int value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_SessionID_set(swigCPtr, this, value);
  }

  public int getSessionID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_SessionID_get(swigCPtr, this);
  }

  public void setInstrumentID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_InstrumentID_set(swigCPtr, this, value);
  }

  public String getInstrumentID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_InstrumentID_get(swigCPtr, this);
  }

  public void setIPAddress(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_IPAddress_set(swigCPtr, this, value);
  }

  public String getIPAddress() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcInputCombActionField_IPAddress_get(swigCPtr, this);
  }

  public CThostFtdcInputCombActionField() {
    this(jctpv6v5v1cpx64apiJNI.new_CThostFtdcInputCombActionField(), true);
  }

}
