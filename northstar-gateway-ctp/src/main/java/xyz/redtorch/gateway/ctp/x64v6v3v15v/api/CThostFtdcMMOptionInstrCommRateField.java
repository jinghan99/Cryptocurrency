/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v3v15v.api;

public class CThostFtdcMMOptionInstrCommRateField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcMMOptionInstrCommRateField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcMMOptionInstrCommRateField obj) {
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
        jctpv6v3v15x64apiJNI.delete_CThostFtdcMMOptionInstrCommRateField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setInstrumentID(String value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_InstrumentID_set(swigCPtr, this, value);
  }

  public String getInstrumentID() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_InstrumentID_get(swigCPtr, this);
  }

  public void setInvestorRange(char value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_InvestorRange_set(swigCPtr, this, value);
  }

  public char getInvestorRange() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_InvestorRange_get(swigCPtr, this);
  }

  public void setBrokerID(String value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_BrokerID_get(swigCPtr, this);
  }

  public void setInvestorID(String value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_InvestorID_set(swigCPtr, this, value);
  }

  public String getInvestorID() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_InvestorID_get(swigCPtr, this);
  }

  public void setOpenRatioByMoney(double value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_OpenRatioByMoney_set(swigCPtr, this, value);
  }

  public double getOpenRatioByMoney() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_OpenRatioByMoney_get(swigCPtr, this);
  }

  public void setOpenRatioByVolume(double value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_OpenRatioByVolume_set(swigCPtr, this, value);
  }

  public double getOpenRatioByVolume() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_OpenRatioByVolume_get(swigCPtr, this);
  }

  public void setCloseRatioByMoney(double value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_CloseRatioByMoney_set(swigCPtr, this, value);
  }

  public double getCloseRatioByMoney() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_CloseRatioByMoney_get(swigCPtr, this);
  }

  public void setCloseRatioByVolume(double value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_CloseRatioByVolume_set(swigCPtr, this, value);
  }

  public double getCloseRatioByVolume() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_CloseRatioByVolume_get(swigCPtr, this);
  }

  public void setCloseTodayRatioByMoney(double value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_CloseTodayRatioByMoney_set(swigCPtr, this, value);
  }

  public double getCloseTodayRatioByMoney() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_CloseTodayRatioByMoney_get(swigCPtr, this);
  }

  public void setCloseTodayRatioByVolume(double value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_CloseTodayRatioByVolume_set(swigCPtr, this, value);
  }

  public double getCloseTodayRatioByVolume() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_CloseTodayRatioByVolume_get(swigCPtr, this);
  }

  public void setStrikeRatioByMoney(double value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_StrikeRatioByMoney_set(swigCPtr, this, value);
  }

  public double getStrikeRatioByMoney() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_StrikeRatioByMoney_get(swigCPtr, this);
  }

  public void setStrikeRatioByVolume(double value) {
    jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_StrikeRatioByVolume_set(swigCPtr, this, value);
  }

  public double getStrikeRatioByVolume() {
    return jctpv6v3v15x64apiJNI.CThostFtdcMMOptionInstrCommRateField_StrikeRatioByVolume_get(swigCPtr, this);
  }

  public CThostFtdcMMOptionInstrCommRateField() {
    this(jctpv6v3v15x64apiJNI.new_CThostFtdcMMOptionInstrCommRateField(), true);
  }

}
