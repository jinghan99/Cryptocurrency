/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v3v15v.api;

public class CThostFtdcQryMMInstrumentCommissionRateField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcQryMMInstrumentCommissionRateField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcQryMMInstrumentCommissionRateField obj) {
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
        jctpv6v3v15x64apiJNI.delete_CThostFtdcQryMMInstrumentCommissionRateField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setBrokerID(String value) {
    jctpv6v3v15x64apiJNI.CThostFtdcQryMMInstrumentCommissionRateField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return jctpv6v3v15x64apiJNI.CThostFtdcQryMMInstrumentCommissionRateField_BrokerID_get(swigCPtr, this);
  }

  public void setInvestorID(String value) {
    jctpv6v3v15x64apiJNI.CThostFtdcQryMMInstrumentCommissionRateField_InvestorID_set(swigCPtr, this, value);
  }

  public String getInvestorID() {
    return jctpv6v3v15x64apiJNI.CThostFtdcQryMMInstrumentCommissionRateField_InvestorID_get(swigCPtr, this);
  }

  public void setInstrumentID(String value) {
    jctpv6v3v15x64apiJNI.CThostFtdcQryMMInstrumentCommissionRateField_InstrumentID_set(swigCPtr, this, value);
  }

  public String getInstrumentID() {
    return jctpv6v3v15x64apiJNI.CThostFtdcQryMMInstrumentCommissionRateField_InstrumentID_get(swigCPtr, this);
  }

  public CThostFtdcQryMMInstrumentCommissionRateField() {
    this(jctpv6v3v15x64apiJNI.new_CThostFtdcQryMMInstrumentCommissionRateField(), true);
  }

}
