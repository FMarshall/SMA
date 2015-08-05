
//Bibliotecas JADE para multiagentes

import jade.core.Agent;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.SubscriptionInitiator;
import jade.proto.SubscriptionResponder;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;
//Bibliotecas JADE para comportamentos temporais
//import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.*;
import jade.util.leap.ArrayList;
import jade.util.leap.Serializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

//import java.util.*;






//Bibliotecas para lidar com arquivos XML
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;








import java.util.Date;
import java.util.Vector;

public class agenteChave extends Agent{
	
	public void setup(){
		
		//Pega o nome do agente no Banco de Dados
		final String agenteChave = getLocalName();
//		final Element agenteChaveBD = carregaBD(agenteChave);
		
		final MessageTemplate fitro_Inform = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchContent("curto"));
		//MessageTemplate fitro_Inform = MessageTemplate.MatchContent("curto");
		
	
		addBehaviour(new TickerBehaviour(this,100) {
			public void onTick(){
				ACLMessage msg = receive(fitro_Inform);
				if(msg != null){
					exibirMensagem(msg);
	
					if(msg.getContent().equalsIgnoreCase("curto")) {
						exibirAviso(myAgent, "detectei um curto");
						
						//seta no seu xml aqui
						
						/********************************************************************************
						 * FIPA Subscribe Initiator para informar que houve falta
						 ********************************************************************************/	
						ACLMessage msgColetarPot = new ACLMessage(ACLMessage.SUBSCRIBE); // Campo da mensagem SUBSCRIBE
						msgColetarPot.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
						msgColetarPot.setContent("falta");
						
						String AL = getLocalName().split("_")[0]; //Só para identificar o seu AL para poder se comunicar
						exibirAviso(myAgent, "Vou avisar da falta para "+AL);
						msgColetarPot.addReceiver(new AID((String) AL, AID.ISLOCALNAME));
											    
					    addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
					    	
							private static final long serialVersionUID = 1L; //Posto automaticamente
						    
							protected void handleAgree(ACLMessage agree){
														
//								exibirAviso(myAgent, "O valor da potencia gerada é: "+PotenciaGeracaoI);
					    	}//Fim do handleAgree do Subscribe
					
							protected void handleRefuse(ACLMessage refuse) { //Se recusar
								
							}// Fim do handleRefuse do Subscribe
							protected void handleFailure(ACLMessage failure) { //Se erro
								
							}// Fim do handleFailure do Subscribe
					    }); // Fim do comportamento FIPA Subscribe -> addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
					    
						
					}
				}
				
				
				
			}//Final do m�todo action do addBehaviour
	    });//Final do comportamento addBehaviour(cyclic Behaviour)
		
		

}//Fim do setup
	
	
	
	
	public void exibirMensagem(ACLMessage msg){
		
		System.out.println("\n\n===============<<MENSAGEM>>==============");
		System.out.println("De: " + msg.getSender());
		System.out.println("Para: " + this.getName());
		System.out.println("Conteudo: " + msg.getContent());
		System.out.println("=============================================");
	}

	
	public void exibirAviso(Agent myAgent, String aviso){
    	
		System.out.println("\n\n-----------------<<AVISO>>------------------");
		System.out.println("Agente: "+myAgent.getLocalName());
		System.out.println("Aviso: " +aviso);
		
		Calendar cal = Calendar.getInstance();
    	cal.getTime();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//    	SimpleDateFormat sdf = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ssss a zzz");
    	SimpleDateFormat sdf = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ssss a");
    	System.out.println( sdf.format(cal.getTime()) );
    	
//		System.out.println(System . currentTimeMillis ());
	}
	
}


