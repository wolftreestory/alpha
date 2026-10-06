package com.alpha.base.config;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.ha.session.ClusterSessionListener;
import org.apache.catalina.ha.session.DeltaManager;
import org.apache.catalina.ha.session.JvmRouteBinderValve;
import org.apache.catalina.ha.tcp.ReplicationValve;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor;
import org.apache.catalina.tribes.membership.McastService;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.transport.ReplicationTransmitter;
import org.apache.catalina.tribes.transport.nio.NioReceiver;
import org.apache.catalina.tribes.transport.nio.PooledParallelSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.alpha.base.config.TomcatClusterConfig.MembershipProperties.MembershipNode;
import com.alpha.base.config.TomcatClusterConfig.MembershipProperties.NodeType;
import com.alpha.base.exception.SystemException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.TomcatClusterConfig).isEnabled() && '${alpha.tomcat-cluster.enabled:false}'.equals('true')")
@ConditionalOnWebApplication
public class TomcatClusterConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Component
	@ConfigurationProperties(prefix="alpha.tomcat-cluster")
	public static class MembershipProperties{

		public enum NodeType {receiver,staticMember}

		private boolean enabled = false;
		private String mcastIp = "228.0.0.4";
		private String mcastPort = "45564";
		private String frequency = "500";
		private String dropTime = "3000";
		private String receiverAddress = "auto";
		private String receiverPort = "5000";

		private List<MembershipNode> nodes;

		public boolean isEnabled() {return enabled;}
		public void setEnabled(boolean enabled) {this.enabled = enabled;}

		public List<MembershipNode> getNodes() {return nodes;}
		public void setNodes(List<MembershipNode> nodes) {this.nodes = nodes;}

		public String getMcastIp() {return mcastIp;}
		public void setMcastIp(String mcastIp) {this.mcastIp = mcastIp;}

		public String getMcastPort() {return mcastPort;}
		public void setMcastPort(String mcastPort) {this.mcastPort = mcastPort;}

		public String getFrequency() {return frequency;}
		public void setFrequency(String frequency) {this.frequency = frequency;}

		public String getDropTime() {return dropTime;}
		public void setDropTime(String dropTime) {this.dropTime = dropTime;}

		public String getReceiverAddress() {return receiverAddress;}
		public void setReceiverAddress(String receiverAddress) {this.receiverAddress = receiverAddress;}

		public String getReceiverPort() {return receiverPort;}
		public void setReceiverPort(String receiverPort) {this.receiverPort = receiverPort;}

		public boolean isStaticMember() {
			if(this.nodes != null && !this.nodes.isEmpty()) {return true;}
			return false;
		}		

		public static class MembershipNode{
			private String name;
			private String host;
			private String port;
			private String uniqueId;
			private NodeType type;
			
			public String getName() {return name;}
			public void setName(String name) {this.name = name;}
			
			public String getHost() {return host;}
			public void setHost(String host) {this.host = host;}
			
			public String getPort() {return port;}
			public void setPort(String port) {this.port = port;}
			
			public String getUniqueId() {return uniqueId;}
			public void setUniqueId(String uniqueId) {this.uniqueId = uniqueId;}
			
			public NodeType getType() {return type;}
			public void setType(NodeType type) {this.type = type;}

			@Override
			public String toString() {
				return "membershipNode [name=" + name + ", host=" + host + ", port=" + port + ", uniqueId=" + uniqueId + ", type=" + type + "]";
			}		
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private Environment environment;
	
	@Autowired
	private MembershipProperties props;
	
	@Value("${server.port:}")
	private String serverPort;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void customize(TomcatServletWebServerFactory factory) {
		
		factory.addContextCustomizers(new TomcatContextCustomizer() {
			
			@Override
			public void customize(Context context) {

				if(props.isStaticMember() && props.getNodes()!=null) {
					props.getNodes().stream().forEach(i -> log.debug(">> {}",i.toString()));
				}

				//[STEP1] 클러스터링시 메시지 송수신에 관련된 하위 component를 그룹핑 
		        GroupChannel channel = null;
		        if(props.isStaticMember()) {
		        	channel = this.getStaticChannel(props.getNodes());
		        } else {
		        	channel = this.getMcatChannel();
		        }
		        
		        //[STEP2] 내장톰켓서버(server.xml)에 cluster설정
		        //org.apache.catalina.ha.tcp.SimpleTcpCluster: 클러스터링 동작
		        
		        //[STEP2.1] SimpleTcpCluster
		        SimpleTcpCluster cluster = new SimpleTcpCluster();
		        cluster.setChannel(channel);
		        cluster.setChannelSendOptions(6);		        
		        cluster.addValve(new ReplicationValve()); //HTTP Request가 끝나는 시점에 다른 복제를 해야할지 말아야 할지 cluster에 알리는 역활
		        cluster.addValve(new JvmRouteBinderValve()); //mod_jk를 사용중 failover시 session에 저장한 jvmWorker속성을 변경하여 다음 request부터는 해당 노드에 고정시킴.     
		        cluster.addClusterListener(new ClusterSessionListener());
		        
		        //[STEP2.2] Engine(server.xml)
		        Engine engine = (Engine)context.getParent().getParent();
		        engine.setCluster(cluster);	
		        
		        ////////////////////////////////////////////////////////////////////////////////////////////////

		        //[STEP3] 웹어플리케이션(web.xml)에 세션복제 활성화 및 세션복제방식 지정     
		        //org.apache.catalina.ha.session.DeltaManager: 모든 노드에 동일한 세션을 복제, 노드 개수가 많을 수록 네트워크 트래픽이 높아지고 메모리 소모 증가
		        //org.apache.catalina.ha.session.BackupManager: Primary Node와 Backup Node로 분리되어 모든 노드에 복제하지 않고 단 Backup Node에만 복제합니다. 하나의 노드에만 복제하기 때문에 DeltaManager의 단점을 커버할 수 있고 failover도 지원.
		        //org.apache.catalina.ha.session.PersistentManager: DB나 파일시스템을 이용하여 세션을 저장합니다. IO문제가 생기기 떄문에 실시간성이 떨어짐.
		        
		        //[STEP3.1] DeltaManager   
		        DeltaManager manager = new DeltaManager();
		        manager.setExpireSessionsOnShutdown(false); //shutdown시 모든 노드의 모든 세션을 expire할지 여부
		        manager.setNotifyListenersOnReplication(true); //다른 tomcat에서 세션이 생성/소멸시 알림을 받을지 여부		        

		        //[STEP3.2] Context(web.xml)
				context.setDistributable(true); 
		        context.setManager(manager);
		        	
			}
		
			private String getServerIp() {
				String hostAddress=null;
				
				if(!StringUtils.isEmpty(serverPort)) {
					try(DatagramSocket socket = new DatagramSocket()) {
						socket.connect(InetAddress.getByName(InetAddress.getLocalHost().getHostName()), Integer.parseInt(serverPort));
						hostAddress = socket.getLocalAddress().getHostAddress();		
						//log.debug("socket.getLocalAddress().getHostAddress():{}",hostAddress);
					} catch (Exception e) {
						log.debug(">> exception skip: {}",e.getMessage());
						//e.printStackTrace();
					}
				}

				if(hostAddress==null || hostAddress.equals("")) {hostAddress="127.0.0.1";}
				return hostAddress;
			}
			
			private GroupChannel getStaticChannel(List<MembershipNode> list) {
				
				String membershipReceiver = environment.getProperty("membership.receiver");
				log.info(">> membershipReceiver(property by jvm option):{}",membershipReceiver);
				
				//auto detect Receiver : nodes 리스트에서 자신의 IP와 같은 것을 검출
				if(StringUtils.isEmpty(membershipReceiver)) {
					String serverIp=this.getServerIp();
					Long count = list.stream().filter(i->i.getHost().equals(serverIp)).count();
					if(count==1) {
						list.forEach(i->{
							if(i.getHost().equals(serverIp)) {
								i.setType(NodeType.receiver);
								log.info(">> membershipReceiver(auto detect):{}",i.getName());
							}
						});
					}
				}

		        //NioReceiver: Cluster로부터 메시지를 수신하는 역활
				NioReceiver receiver = new NioReceiver();				
				
				//StaticMembershipInterceptor
				StaticMembershipInterceptor staticMembershipInterceptor = new StaticMembershipInterceptor();
	
				//UniqueId 중복체크, host:port 중복체크, receiver 중복체크
				Set<String> checkSet = new HashSet<>();
				list.forEach(i->{
					if(StringUtils.isBlank(i.getName())) {throw new SystemException("node:name is null or empty.");}
					if(StringUtils.isBlank(i.getHost())) {throw new SystemException("node:host is null or empty.");}
					if(StringUtils.isBlank(i.getPort())) {throw new SystemException("node:port is null or empty.");}
					if(StringUtils.isBlank(i.getUniqueId())) {throw new SystemException("node:UniqueId is null or empty.");}
					if(i.getType()==null) {i.setType(NodeType.staticMember);} //default

					if(checkSet.contains(i.getName())) {throw new SystemException("node:name is duplication.");}	
					checkSet.add(i.getName());

					String member=i.getHost()+":"+i.getPort();
					if(checkSet.contains(member)) {throw new SystemException("node:port is duplication.");}					
					checkSet.add(member);
					
					if(checkSet.contains(i.getUniqueId())) {throw new SystemException("node:uniqueId is duplication.");}	
					checkSet.add(i.getUniqueId());

					if(i.getType().equals(NodeType.receiver)) {
						if(checkSet.contains(NodeType.receiver.toString())) {throw new SystemException("node:type(receiver) is only one.");}	
						checkSet.add(NodeType.receiver.toString());					
					}
					
					if(!StringUtils.isBlank(membershipReceiver) && i.getName().equals(membershipReceiver)) {
						i.setType(NodeType.receiver);
						checkSet.add(NodeType.receiver.toString());
					}
					 log.info(">> node:{}",i.toString());
				});
				
				
				//receiver 존재여부체크
				if(!checkSet.contains(NodeType.receiver.toString())) {
					throw new SystemException("membershipNode:type(receiver) is not exist. JVM option -Dmembership.receiver=[node.name] or One of the settings in application properties 'tomcatcluster.nodes' should be 'receiver'.");
				}
				
				//구성: staticMembershipInterceptor, receiver
				list.forEach(i->{					
					if(i.getType().equals(NodeType.staticMember)) {
						StaticMember staticMember = new StaticMember();
				        staticMember.setUniqueId(i.getUniqueId());
				        staticMember.setHost(i.getHost());
				        staticMember.setPort(Integer.parseInt(i.getPort()));
				        staticMember.setSecurePort(-1); //default
				        staticMembershipInterceptor.addStaticMember(staticMember);
				        
				        log.info(">> staticMember:{}",i.toString());
					}
					if(i.getType().equals(NodeType.receiver)) {
				        receiver.setAddress(i.getHost());
				        receiver.setPort(Integer.parseInt(i.getPort()));
				        receiver.setMaxThreads(6); //non-blocking, 기본적으로 노드당 1개의 thread를 할당 default(min:6, max:15)
				        
				        log.info(">> receiver:{}",i.toString());
					}
				});
				
		        //ReplicationTransmitter: 노드에서 Cluster로 메시지를 보내는 역활 (사실상 빈 껍데기로 상세 역확을 Transport에서 정의)
		        ReplicationTransmitter channelSender = new ReplicationTransmitter();
		        channelSender.setTransport(new PooledParallelSender());	//non-blocking, 여러 노드로 메시지를 전송, 하나의 노드에 여러 메시지를 동시전송
		        
		        //GroupChannel: 클러스터링시 메시지 송수신에 관련된 하위 component를 그룹핑(membership, sender/transport, receiver, interceptor)		        
				GroupChannel channel = new GroupChannel();
				channel.setChannelReceiver(receiver);
				channel.setChannelSender(channelSender);
				channel.addInterceptor(staticMembershipInterceptor);
		        channel.addInterceptor(new TcpPingInterceptor());
		        channel.addInterceptor(new TcpFailureDetector());
		        channel.addInterceptor(new MessageDispatchInterceptor());

				return channel;
			}
			
			private GroupChannel getMcatChannel() {
				
				log.debug(">> membership.mcastIp:{}",props.getMcastIp());
				log.debug(">> membership.mcastPort:{}",props.getMcastPort());
				log.debug(">> membership.frequency:{}",props.getFrequency());
				log.debug(">> membership.castDropTime:{}",props.getDropTime());
				
		        //McastService: 클러스터에 참여한 노드에서 활성노드 파악(분별),그룹내 1대N UDP통신.
		        McastService membershipService = new McastService();
		        membershipService.setAddress(props.getMcastIp());
		        membershipService.setPort(Integer.parseInt(props.getMcastPort())); //TCP&UDP port 오픈 필요
		        membershipService.setFrequency(Integer.parseInt(props.getFrequency())); //frequency에 설정된 간격으로 각 노드들이 UDP packet을 날려 heartbeat 확인.
		        membershipService.setDropTime(Integer.parseInt(props.getDropTime())); //dropTime에 설정된 시간동안 HeartBeat이 없을 경우 장애로 판단하고 각 노드에 알림.
		        
		        //ReplicationTransmitter: 노드에서 Cluster로 메시지를 보내는 역활 (사실상 빈 껍데기로 상세 역확을 Transport에서 정의)
		        ReplicationTransmitter channelSender = new ReplicationTransmitter();
		        channelSender.setTransport(new PooledParallelSender()); //non-blocking, 여러 노드로 메시지를 전송, 하나의 노드에 여러 메시지를 동시전송
		        
		        //NioReceiver: Cluster로부터 메시지를 수신하는 역활, non-blocking
		        NioReceiver channelReceiver = new NioReceiver();
		        channelReceiver.setAddress(props.getReceiverAddress()); //tomcat_IP (auto: java.net.InetAddress.getLocalHost().getHostAddress())
		        channelReceiver.setPort(Integer.parseInt(props.getReceiverPort())); //TCP port 오픈 필요
		        channelReceiver.setMaxThreads(6); //기본적으로 노드당 1개의 thread를 할당 default(min:6, max:15)
		        
		        //GroupChannel: 클러스터링시 메시지 송수신에 관련된 하위 component를 그룹핑(membership, sender/transport, receiver, interceptor)
		        GroupChannel channel = new GroupChannel();
		        channel.setMembershipService(membershipService);
		        channel.setChannelReceiver(channelReceiver);
		        channel.setChannelSender(channelSender);
		        channel.addInterceptor(new TcpPingInterceptor()); //클러스터 그룹내 타 멤버가 정말 중지한 것이 맞는지 TCP Unicast를 통해 확인.
		        channel.addInterceptor(new TcpFailureDetector()); //클러스터 그룹 내 특정 멤버가 장애나 재기동에 의해 중지되면 그룹내 다른 멤버가 중지한 멤버 내 세션들을 이어받아 처리하게 된다. 이때 세션 ID가 jvmRoute를 포함하고 있다면 JvmRouteBinderValve는 이전 멤버의 jvmRoute값을 새로운 멤버의 jvmRoute로 변경		        
		        channel.addInterceptor(new MessageDispatchInterceptor());
		        
		        return channel;
			}
		});
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}