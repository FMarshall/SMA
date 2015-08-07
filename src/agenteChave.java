
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
import java.io.File;
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
		final Element agenteCBD = carregaBD(agenteChave);
		
		final MessageTemplate fitro_Inform = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchContent("curto"));
		
		final MessageTemplate filtroAbrir = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
  		MessageTemplate.MatchContent("abra")); 
		
	
		addBehaviour(new TickerBehaviour(this,100) {
			public void onTick(){
				ACLMessage msg = receive(fitro_Inform);
				if(msg != null){
					exibirMensagem(msg);
	
					if(msg.getContent().equalsIgnoreCase("curto")) {
						exibirAviso(myAgent, "detectei um curto");
						
						agenteCBD.getChild("estado").setText("0"); //seto a TAG "estado" do XML como aberta ("0")
						agenteCBD.getChild("comando").setText("0"); //quando o agente  for responder ao matlab enviando um sinal de comando ele irá querer a chave continuando aberta
						
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
					    
						
					}// Fim do msg é curto
					else { //Se não for curto, então recebo o valor de potência do trecho para atualização
						/*
						 * Parte de medição e aquisição de dados e armazenamento no XML
						 */
						String carga = msg.getContent();  //Pego o conteudo da mensagem
						exibirAviso(myAgent, "O conteúdo da msg que recebi é: "+carga);
						
						//O conteudo do agente chave é somente o valor de corrente demandada no seu trecho
						agenteCBD.getChild("carga").setText(carga);	//seta o XML do agente atualizando o valor da corrente demandada
						
						/*
						 * Parte de consulta ao XML e comando
						 * O camando será enviado como resposta ao inform da medição. 
						 */
						String comandoChave = agenteCBD.getChild("comando").getText(); //Consulta no XML o valor do disjuntor a jusante do inversor
												
						ACLMessage resposta = msg.createReply();
						resposta.setContent(comandoChave); //seta o conteudo da mensagem como o comando da chave que poderá ser aberta ou fechada
						send(resposta);  //enviando a mensagem de resposta do Inform ao Matlalb
						
						/********************************************************************************
						 * FIPA Subscribe Initiator para informar o valor atual de carga demandada
						 ********************************************************************************/	
						ACLMessage msgInformarPot = new ACLMessage(ACLMessage.SUBSCRIBE); // Campo da mensagem SUBSCRIBE
						msgInformarPot.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
						
						msgInformarPot.setContent(carga);
						
						String AL = agenteChave.split("_")[0].split("AL")[0]; //Só para identificar o seu AL para poder se comunicar
						exibirAviso(myAgent, "Vou informar o valor de carga demandada à "+AL);
						msgInformarPot.addReceiver(new AID((String) AL, AID.ISLOCALNAME));
											    
					    addBehaviour(new SubscriptionInitiator(myAgent,msgInformarPot){
					    	
							protected void handleAgree(ACLMessage agree){
														
					    	}//Fim do handleAgree do Subscribe
					
							protected void handleRefuse(ACLMessage refuse) { //Se recusar
								
							}// Fim do handleRefuse do Subscribe
							protected void handleFailure(ACLMessage failure) { //Se erro
								
							}// Fim do handleFailure do Subscribe
					    }); // Fim do comportamento FIPA Subscribe -> addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
						
					}// Fim do se é curto senão é para atualização do valor de potência nos trechos
				}// Fim do if para saber se mensagem != null
				
				
				
			}//Final do m�todo action do addBehaviour
	    });//Final do comportamento addBehaviour(cyclic Behaviour)
		
		/*************************************************************************
		 * FIPA Request Responder para responder a solicitação do AL para abrir
		 **************************************************************************/
		addBehaviour(new AchieveREResponder(this, filtroAbrir) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
//				System.out.println("Agent "+getLocalName()+ ": REQUEST received from "+request.getSender().getName()+". Action is "+request.getContent());
//				if (checkAction()) {
//					// We agree to perform the action. Note that in the FIPA-Request
//					// protocol the AGREE message is optional. Return null if you
//					// don't want to send it.
//					System.out.println("Agent "+getLocalName()+": Agree");
//					ACLMessage agree = request.createReply();
//					agree.setPerformative(ACLMessage.AGREE);
//					return agree;
//				}
//				else {
//					// We refuse to perform the action
//					System.out.println("Agent "+getLocalName()+": Refuse");
//					throw new RefuseException("check-failed");
//				}
				exibirAviso(myAgent, "Agent "+getLocalName()+ ": REQUEST received from "+request.getSender().getName()+". Action is "+request.getContent());
				
				ACLMessage resposta = request.createReply();
				
				//Antes seto no XML que o agente chave irá comandar a abertura da sua chave quando for responder ao inform de monitoramento do matlab
				agenteCBD.getChild("comando").setText("0");
				
				return resposta;
			}// Fim do protected ACLMessage prepareResponse
		} );//Fim do request responder 
		
//		/********************************************************************************
//		 * FIPA Subscribe Initiator para informar o valor atual de carga demandada
//		 ********************************************************************************/	
//		ACLMessage msgInformarPot = new ACLMessage(ACLMessage.SUBSCRIBE); // Campo da mensagem SUBSCRIBE
//		msgInformarPot.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
//		
//		String carga = agenteCBD.getChildText("carga"); //Carga do trecho monitorado
//		msgInformarPot.setContent(carga);
//		
//		String AL = getLocalName().split("_")[0]; //Só para identificar o seu AL para poder se comunicar
//		exibirAviso(this, "Vou informar o valor de carga demandada à "+AL);
//		msgInformarPot.addReceiver(new AID((String) AL, AID.ISLOCALNAME));
//							    
//	    addBehaviour(new SubscriptionInitiator(this,msgInformarPot){
//	    	
//			protected void handleAgree(ACLMessage agree){
//										
//	    	}//Fim do handleAgree do Subscribe
//	
//			protected void handleRefuse(ACLMessage refuse) { //Se recusar
//				
//			}// Fim do handleRefuse do Subscribe
//			protected void handleFailure(ACLMessage failure) { //Se erro
//				
//			}// Fim do handleFailure do Subscribe
//	    }); // Fim do comportamento FIPA Subscribe -> addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){

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
	
	/**
	 *  Método para carregamento do XML
	 * 
	 */
	public Element carregaBD(String nomeAgente) {

		// Declaração de variáveis, uma de cada tipo
		File endereco = null; // Desnecessário se usarmos o endereço do arq.
								// dentro do doc dentro do try
		SAXBuilder builder = null; // usado p/ processar a estrut. do doc. p/
									// dentro da variável do tipo documento
		Document doc = null;
		Element BD = null;

		endereco = new File("src/XML/" + nomeAgente + ".xml");

		builder = new SAXBuilder();

		try { // Criando o arquivo propriamente dito
			doc = builder.build(endereco);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}// fim do try

		BD = doc.getRootElement();
		return BD;

	}// fim do método carregarBD
	
}


