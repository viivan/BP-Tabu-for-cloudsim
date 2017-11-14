/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * EDIT BY VIIVAN AND LIJIE
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package tabu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.lists.VmList;

import com.sun.xml.internal.messaging.saaj.util.TeeInputStream;

/**datacenter ���?����vm���?��vm�������ύcloudlet����vm���
 * DatacentreBroker represents a broker acting on behalf of a user. It hides VM management, as vm
 * creation, sumbission of cloudlets to this VMs and destruction of VMs.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class DatacenterBroker extends SimEntity {

	/** The vm list. vm�б�*/
	protected List<? extends Vm> vmList;

	/** The vms created list. ������vm�б�*/
	protected List<? extends Vm> vmsCreatedList;

	/** The cloudlet list. cloudlet�б�*/
	protected List<? extends Cloudlet> cloudletList;

	/** The cloudlet submitted list. cloudlet�ύ�б�*/
	protected List<? extends Cloudlet> cloudletSubmittedList;

	/** The cloudlet received list.�յ���cloudlet�б� */
	protected List<? extends Cloudlet> cloudletReceivedList;

	/** The cloudlets submitted. �ύ��cloudlet��*/
	protected int cloudletsSubmitted;

	/** The vms requested. �����vm��*/
	protected int vmsRequested;

	/** The vms acks.vm����Ӧ�� */
	protected int vmsAcks;

	/** The vms destroyed. ��ٵ�vm��*/
	protected int vmsDestroyed;

	/** The datacenter ids list. �������id�б�*/
	protected List<Integer> datacenterIdsList;

	/** The datacenter requested ids list. ������������id�б�*/
	protected List<Integer> datacenterRequestedIdsList;

	/** The vms to datacenters map. �����������ĵ�ӳ��*/
	protected Map<Integer, Integer> vmsToDatacentersMap;

	/** The datacenter characteristics list. ������������б�*/
	protected Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList;

	/**
	 * Created a new DatacenterBroker object.
	 * 
	 * @param name name to be associated with this entity (as required by Sim_entity class from
	 *            simjava package)
	 * @throws Exception the exception
	 * @pre name != null
	 * @post $none
	 */
	// ����������Ĵ���
	public DatacenterBroker(String name) throws Exception {
		super(name);

		setVmList(new ArrayList<Vm>());
		setVmsCreatedList(new ArrayList<Vm>());
		setCloudletList(new ArrayList<Cloudlet>());
		setCloudletSubmittedList(new ArrayList<Cloudlet>());
		setCloudletReceivedList(new ArrayList<Cloudlet>());

		cloudletsSubmitted = 0;
		setVmsRequested(0);
		setVmsAcks(0);
		setVmsDestroyed(0);

		setDatacenterIdsList(new LinkedList<Integer>());
		setDatacenterRequestedIdsList(new ArrayList<Integer>());
		setVmsToDatacentersMap(new HashMap<Integer, Integer>());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());
	}

	/**
	 * This method is used to send to the broker the list with virtual machines that must be
	 * created.
	 * 
	 * @param list the list
	 * @pre list !=null
	 * @post $none
	 */
	//�ύvm
	public void submitVmList(List<? extends Vm> list) {
		getVmList().addAll(list);
	}

	/**
	 * This method is used to send to the broker the list of cloudlets.
	 * 
	 * @param list the list
	 * @pre list !=null
	 * @post $none
	 */
	//�ύcloudlet
	public void submitCloudletList(List<? extends Cloudlet> list) {
		getCloudletList().addAll(list);
	}

	/**
	 * Specifies that a given cloudlet must run in a specific virtual machine.
	 * 
	 * @param cloudletId ID of the cloudlet being bount to a vm
	 * @param vmId the vm id
	 * @pre cloudletId > 0
	 * @pre id > 0
	 * @post $none
	 */
	//ָ��cloudlet��ָ��vm������
	public void bindCloudletToVm(int cloudletId, int vmId) {
		CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);
	}

	/**
	 * Processes events available for this Broker.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	//ÿ��ʵ���඼������processEvent()���������������Լ���ص��¼�
	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// Resource characteristics request
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				processResourceCharacteristicsRequest(ev);
				break;
			// Resource characteristics answer
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				processResourceCharacteristics(ev);
				break;
			// VM Creation answer
			case CloudSimTags.VM_CREATE_ACK:
				processVmCreate(ev);
				break;
			// A finished cloudlet returned
			case CloudSimTags.CLOUDLET_RETURN:
				processCloudletReturn(ev);
				break;
			// if the simulation finishes
			case CloudSimTags.END_OF_SIMULATION:
				shutdownEntity();
				break;
			// other unknown tags are processed by this method
			default:
				processOtherEvent(ev);
				break;
		}
	}

	/**
	 * Process the return of a request for the characteristics of a PowerDatacenter.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
		getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
			setDatacenterRequestedIdsList(new ArrayList<Integer>());
			createVmsInDatacenter(getDatacenterIdsList().get(0));
		}
	}

	/**
	 * Process a request for the characteristics of a PowerDatacenter.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristicsRequest(SimEvent ev) {
		setDatacenterIdsList(CloudSim.getCloudResourceList());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());

		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloud Resource List received with "
				+ getDatacenterIdsList().size() + " resource(s)");

		for (Integer datacenterId : getDatacenterIdsList()) {
			sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
		}
	}

	/**
	 * Process the ack received due to a request for VM creation.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
			Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId
					+ " has been created in Datacenter #" + datacenterId + ", Host #"
					+ VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
		} else {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
					+ " failed in Datacenter #" + datacenterId);
		}

		incrementVmsAcks();

		// all the requested VMs have been created
		if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
			submitCloudlets();
		} else {
			// all the acks received, but some VMs were not created
			if (getVmsRequested() == getVmsAcks()) {
				// find id of the next datacenter that has not been tried
				for (int nextDatacenterId : getDatacenterIdsList()) {
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				// all datacenters already queried
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();
				} else { // no vms created. abort
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();
				}
			}
		}
	}

	/**
	 * Process a cloudlet return event.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
				+ " received");
		cloudletsSubmitted--;
		if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
			Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { // some cloudlets haven't finished yet
			if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
				// all the cloudlets sent finished. It means that some bount
				// cloudlet is waiting its VM be created
				clearDatacenters();
				createVmsInDatacenter(0);
			}

		}
	}

	/**
	 * Overrides this method when making a new and different type of Broker. This method is called
	 * by {@link #body()} for incoming unknown tags.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processOtherEvent(SimEvent ev) {
		if (ev == null) {
			Log.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null.");
			return;
		}

		Log.printLine(getName() + ".processOtherEvent(): "
				+ "Error - event unknown by this DatacenterBroker.");
	}

	/**
	 * Create the virtual machines in a datacenter.
	 * 
	 * @param datacenterId Id of the chosen PowerDatacenter
	 * @pre $none
	 * @post $none
	 */
	protected void createVmsInDatacenter(int datacenterId) {
		// send as much vms as possible for this datacenter before trying the next one
		int requestedVms = 0;
		String datacenterName = CloudSim.getEntityName(datacenterId);
		for (Vm vm : getVmList()) {
			if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
						+ " in " + datacenterName);
				sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
				requestedVms++;
			}
		}

		getDatacenterRequestedIdsList().add(datacenterId);

		setVmsRequested(requestedVms);
		setVmsAcks(0);
	}

	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		int vmIndex = 0;
		for (Cloudlet cloudlet : getCloudletList()) {
			Vm vm;
			// if user didn't bind this cloudlet and it has not been executed yet
			if (cloudlet.getVmId() == -1) {
				vm = getVmsCreatedList().get(vmIndex);
			} else { // submit to the specific vm
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
				if (vm == null) { // vm was not created
					Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
							+ cloudlet.getCloudletId() + ": bount VM not available");
					continue;
				}
			}

			Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
					+ cloudlet.getCloudletId() + " to VM #" + vm.getId());
			cloudlet.setVmId(vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
			cloudletsSubmitted++;
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
			getCloudletSubmittedList().add(cloudlet);
		}

		// remove submitted cloudlets from waiting list
		for (Cloudlet cloudlet : getCloudletSubmittedList()) {
			getCloudletList().remove(cloudlet);
		}
	}

	/**
	 * Destroy the virtual machines running in datacenters.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void clearDatacenters() {
		for (Vm vm : getVmsCreatedList()) {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Destroying VM #" + vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
		}

		getVmsCreatedList().clear();
	}

	/**
	 * Send an internal event communicating the end of the simulation.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void finishExecution() {
		sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.core.SimEntity#shutdownEntity()
	 */
	@Override
	public void shutdownEntity() {
		Log.printLine(getName() + " is shutting down...");
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.core.SimEntity#startEntity()
	 */
	//������ʵ��
	@Override
	public void startEntity() {
		Log.printLine(getName() + " is starting...");
		//ͨ��id���������һ��ʵ�巢���¼������������������
		schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
	}

	/**
	 * Gets the vm list.
	 * 
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmList() {
		return (List<T>) vmList;
	}

	/**����������б�
	 * Sets the vm list.
	 * 
	 * @param <T> the generic type
	 * @param vmList the new vm list
	 */
	protected <T extends Vm> void setVmList(List<T> vmList) {
		this.vmList = vmList;
	}

	/**
	 * Gets the cloudlet list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletList() {
		return (List<T>) cloudletList;
	}

	/**
	 * Sets the cloudlet list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletList the new cloudlet list
	 */
	protected <T extends Cloudlet> void setCloudletList(List<T> cloudletList) {
		this.cloudletList = cloudletList;
	}

	/**
	 * Gets the cloudlet submitted list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet submitted list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletSubmittedList() {
		return (List<T>) cloudletSubmittedList;
	}

	/**
	 * Sets the cloudlet submitted list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletSubmittedList the new cloudlet submitted list
	 */
	protected <T extends Cloudlet> void setCloudletSubmittedList(List<T> cloudletSubmittedList) {
		this.cloudletSubmittedList = cloudletSubmittedList;
	}

	/**
	 * Gets the cloudlet received list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet received list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletReceivedList() {
		return (List<T>) cloudletReceivedList;
	}

	/**
	 * Sets the cloudlet received list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletReceivedList the new cloudlet received list
	 */
	protected <T extends Cloudlet> void setCloudletReceivedList(List<T> cloudletReceivedList) {
		this.cloudletReceivedList = cloudletReceivedList;
	}

	/**
	 * Gets the vm list.
	 * 
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmsCreatedList() {
		return (List<T>) vmsCreatedList;
	}

	/**
	 * Sets the vm list.
	 * 
	 * @param <T> the generic type
	 * @param vmsCreatedList the vms created list
	 */
	protected <T extends Vm> void setVmsCreatedList(List<T> vmsCreatedList) {
		this.vmsCreatedList = vmsCreatedList;
	}

	/**
	 * Gets the vms requested.
	 * 
	 * @return the vms requested
	 */
	protected int getVmsRequested() {
		return vmsRequested;
	}

	/**
	 * Sets the vms requested.
	 * 
	 * @param vmsRequested the new vms requested
	 */
	protected void setVmsRequested(int vmsRequested) {
		this.vmsRequested = vmsRequested;
	}

	/**
	 * Gets the vms acks.
	 * 
	 * @return the vms acks
	 */
	protected int getVmsAcks() {
		return vmsAcks;
	}

	/**
	 * Sets the vms acks.
	 * 
	 * @param vmsAcks the new vms acks
	 */
	protected void setVmsAcks(int vmsAcks) {
		this.vmsAcks = vmsAcks;
	}

	/**
	 * Increment vms acks.
	 */
	protected void incrementVmsAcks() {
		vmsAcks++;
	}

	/**
	 * Gets the vms destroyed.
	 * 
	 * @return the vms destroyed
	 */
	protected int getVmsDestroyed() {
		return vmsDestroyed;
	}

	/**
	 * Sets the vms destroyed.
	 * 
	 * @param vmsDestroyed the new vms destroyed
	 */
	protected void setVmsDestroyed(int vmsDestroyed) {
		this.vmsDestroyed = vmsDestroyed;
	}

	/**
	 * Gets the datacenter ids list.
	 * 
	 * @return the datacenter ids list
	 */
	protected List<Integer> getDatacenterIdsList() {
		return datacenterIdsList;
	}

	/**
	 * Sets the datacenter ids list.
	 * 
	 * @param datacenterIdsList the new datacenter ids list
	 */
	protected void setDatacenterIdsList(List<Integer> datacenterIdsList) {
		this.datacenterIdsList = datacenterIdsList;
	}

	/**
	 * Gets the vms to datacenters map.
	 * 
	 * @return the vms to datacenters map
	 */
	protected Map<Integer, Integer> getVmsToDatacentersMap() {
		return vmsToDatacentersMap;
	}

	/**
	 * Sets the vms to datacenters map.
	 * 
	 * @param vmsToDatacentersMap the vms to datacenters map
	 */
	protected void setVmsToDatacentersMap(Map<Integer, Integer> vmsToDatacentersMap) {
		this.vmsToDatacentersMap = vmsToDatacentersMap;
	}

	/**
	 * Gets the datacenter characteristics list.
	 * 
	 * @return the datacenter characteristics list
	 */
	protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsList() {
		return datacenterCharacteristicsList;
	}

	/**
	 * Sets the datacenter characteristics list.
	 * 
	 * @param datacenterCharacteristicsList the datacenter characteristics list
	 */
	protected void setDatacenterCharacteristicsList(
			Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList) {
		this.datacenterCharacteristicsList = datacenterCharacteristicsList;
	}

	/**
	 * Gets the datacenter requested ids list.
	 * 
	 * @return the datacenter requested ids list
	 */
	protected List<Integer> getDatacenterRequestedIdsList() {
		return datacenterRequestedIdsList;
	}

	/**
	 * Sets the datacenter requested ids list.
	 * 
	 * @param datacenterRequestedIdsList the new datacenter requested ids list
	 */
	protected void setDatacenterRequestedIdsList(List<Integer> datacenterRequestedIdsList) {
		this.datacenterRequestedIdsList = datacenterRequestedIdsList;
	} 
	
	
	private int cloudletNum;
	private int vmNum;
	private int [][] plan ;
	double vl_rg,mis = 0,mipss = 0;
	//BP-Tabu算法
	public void bindUseTabu(){
		cloudletNum = cloudletList.size();
		vmNum = vmList.size();
		//初始化分配方案全为0
		plan = new int[cloudletNum][vmNum];
		for(int i = 0 ; i < cloudletNum;i++){
			for(int j = 0;j < vmNum;j++){
				plan[i][j] = 0;
				mipss+=vmList.get(j).getMips();
			}
			mis += cloudletList.get(i).getCloudletLength();
		}
		vl_rg = mis / mipss;
//		timeAwared();
//		
//		tabu();
		for(int p = 0 ; p < cloudletNum;p++){
			for(int k = 0;k < vmNum;k++){
				if(plan[p][k] == 1){
					cloudletList.get(p).setVmId(vmList.get(k).getId());
				}
			}
		}
	}

	double [][] ct ;
	@SuppressWarnings("unchecked")
	private void timeAwared() {
		//time[i][j] 表示任务i在虚拟机j上的执行时间
		ct = new double[cloudletNum][vmNum];
		//cloudletList按MI降序排列, vm按MIPS升序排列
		Collections.sort(cloudletList,new CloudletComparator());
		Collections.sort(vmList,new VmComparator());
		
		for(int i=0;i<cloudletNum;i++){
			for(int j=0;j<vmNum;j++){
				ct[i][j] = (double)cloudletList.get(i).getCloudletLength()/vmList.get(j).getMips();
			}
		}
		//在某个虚拟机上任务的总执行时间
		double[] vmLoad=new double[vmNum];
		//在某个Vm上运行的任务数量
		int[] vmTasks=new int[vmNum]; 
		//记录当前任务分配方式的最优值
		double minLoad=0;
		//记录当前任务最优分配方式对应的虚拟机列号
		int idx=0;
		//第一个cloudlet分配给最快的vm
		vmLoad[vmNum-1]=ct[0][vmNum-1];
		vmTasks[vmNum-1]=1;
//		cloudletList.get(0).setVmId(vmList.get(vmNum-1).getId());
		plan[0][vmNum-1] = 1;
		for(int h=1;h<cloudletNum;h++){
			minLoad=vmLoad[vmNum-1]+ct[h][vmNum-1];
			idx=vmNum-1;
			for(int j=vmNum-2;j>=0;j--){
				//如果当前虚拟机未分配任务，则比较完当前任务分配给该虚拟机是否最优
				if(vmLoad[j]==0){
					if(minLoad>=ct[h][j])
						idx=j;
					break;
				}
				if(minLoad>vmLoad[j]+ct[h][j]){
					minLoad=vmLoad[j]+ct[h][j];
					idx=j;
				}
				//简单的负载均衡
				else if(minLoad==vmLoad[j]+ct[h][j]&&vmTasks[j]<vmTasks[idx])
					idx=j;
			}
			vmLoad[idx]+=ct[h][idx];
			vmTasks[idx]++;
//			cloudletList.get(i).setVmId(vmList.get(idx).getId());
			plan[h][idx] = 1;
		}
	}
	// 自定义比较器：按cloudlet降排序
    static class CloudletComparator implements Comparator {  
        public int compare(Object object1, Object object2) {// 实现接口中的方法  
        	Cloudlet c1 = (Cloudlet) object1; // 强制转换  
        	Cloudlet c2 = (Cloudlet) object2;  
            return (int)(c2.getCloudletLength() - c1.getCloudletLength());  
        }  
    }
    
    // 自定义比较器：按vm的MIPS升序排序
    static class VmComparator implements Comparator {  
        public int compare(Object object1, Object object2) {// 实现接口中的方法  
        	Vm v1 = (Vm) object1; // 强制转换  
        	Vm v2 = (Vm) object2;  
            return (int)(v1.getMips() - v2.getMips());  
        }  
    }
    
    //bp-tabu
	int k;
	int pt;
	//vmmax，vmmin的索引
	int idxmax = 0,idxmin = 0;
	double vmmax = 0,vmmin = 0;
	int [] tabuList;
	ArrayList<Integer> vmmaxIdx = new ArrayList<Integer>();
	ArrayList<Integer> vmminIdx = new ArrayList<Integer>();
	private void tabu() {
		k = 10;
		pt = 0;
		tabuList = new int[cloudletNum];
		for (int i = 0;i < cloudletNum;i++){
			tabuList[i] = 1;
		}
		do {
			getVMmaxVMmin();
			if(pt >= k){
				//惩戒策略
				publishment();
			}
			//多因素收益值函数进行交换
			if(idxmin != idxmax){
				changeByBenefit();
			}
		}while (isFinishSchedualing());
	}



	private void changeByBenefit() {
		//定义交换最终交换的索引
		int changeMaxIndex,changeMinIndex;
		//定义临时存放的交换索引
		int tempIndex,tempMaxIndex = -1,tempMinIndex = -1;
		//用于交换
		int temp;
		//临时保存的优值以及最终优值
		double maxBenefit,tempBenefit;
		maxBenefit = getLB();
		//寻找交换后最优的任务对索引
		for (int maxIndex : vmmaxIdx){
			if(tabuList[maxIndex] == 1){
				//tempMaxIndex = maxIndex;
				for (int minIndex : vmminIdx){
					if(tabuList[minIndex] == 1){
						//tempMinIndex = minIndex;
//						temp = plan[minIndex][idxmin];
						plan[minIndex][idxmin] = plan[maxIndex][idxmax] = 0;
						plan[minIndex][idxmax] = plan[maxIndex][idxmin] = 1;
						tempBenefit = getLB();
						if (tempBenefit < maxBenefit) {
							maxBenefit = tempBenefit;
							tempMaxIndex = maxIndex;
							tempMinIndex = minIndex;
//							changeMaxIndex = tempMaxIndex;
//							changeMinIndex = tempMinIndex;
						}
						plan[minIndex][idxmin] = plan[maxIndex][idxmax] = 1;
						plan[minIndex][idxmax] = plan[maxIndex][idxmin] = 0;
					}
				}
			}
		}
		if (tempMinIndex != -1){
			tabuList[tempMaxIndex] = 0;
			tabuList[tempMinIndex] = 0;
			
			plan[tempMinIndex][idxmin] = plan[tempMaxIndex][idxmax] = 0;
			plan[tempMinIndex][idxmax] = plan[tempMaxIndex][idxmin] = 1;
		} else {
			pt++;
		}
	}

	private double getLB() {
		double lb = 0;
		lb = getVL() / vmNum;
		return lb;
	}

	private double getVL() {
		double vl = 0;
		for(int j = 0; j < vmNum ; j++){
			double vlj = 0;
			for(int i = 0; i < cloudletNum; i++){
				vlj += plan[i][j] * ct[i][j];
			}
			vl += (vlj-vl_rg) * (vlj-vl_rg);
		}
		return vl;
	}

	private boolean isFinishSchedualing() {
		boolean tag = true;
		for( int i : vmmaxIdx){
			if (tabuList[i] == 1) {
				tag = false;
			}
		}
		for( int j : vmminIdx){
			if (tabuList[j] == 1) {
				tag = false;
			}
		}
		return tag;
	}

	private void getVMmaxVMmin() {
		for(int j = 0;j < vmNum;j++){
			double vmtemp = 0;
			for(int k = 0;k < cloudletNum;k++){
				vmtemp += plan[k][j] * ct[k][j];
			}
			if(vmtemp > vmmax){
				vmmax = vmtemp;
				idxmax = j;
			}
			if(vmmin == 0){
				vmmin = vmtemp;
			}
			if(vmtemp < vmmin){
				vmmin = vmtemp;
				idxmin = j;
			}
		}
		for(int l = 0; l < cloudletNum; l++){
			if(plan[l][idxmax] == 1){
				vmmaxIdx.add(l);
			}
			if(plan[l][idxmin] == 1){
				vmminIdx.add(l);
			}
		}
	}

	private void publishment() {
		vmmin -= 1;
	}

}
