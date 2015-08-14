
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

public class agenteEncontro extends Agent{
	
	public void setup(){
		
		//Pega o nome do agente no Banco de Dados
		final String agenteChave = getLocalName();
		final Element agenteCBD = carregaBD(agenteChave);
		
//		final MessageTemplate fitro_Inform = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchContent("curto"));
		final MessageTemplate fitro_Inform = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
		final MessageTemplate filtroAbrir = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
  		MessageTemplate.MatchContent("fechar")); 
		
	
		addBehaviour(new TickerBehaviour(this,100) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void onTick(){
				ACLMessage msg = receive(fitro_Inform);
				
				if(msg != null){
//					exibirMensagem(msg);
										
					String comandoChave = agenteCBD.getChild("comando").getText(); //Consulta no XML o valor do disjuntor a jusante do inversor
					
					ACLMessage resposta = msg.createReply();
					resposta.setContent(comandoChave); //seta o conteudo da mensagem como o comando da chave que poderá ser aberta ou fechada
				    send(resposta);  //enviando a mensagem de resposta do Inform ao Matlalb
					
					
//					
				}// Fim do if para saber se mensagem != null
			}//Final do m�todo action do addBehaviour
	    });//Final do comportamento addBehaviour(cyclic Behaviour)
		
		/*************************************************************************
		 * FIPA Request Responder para responder a solicitação do AL para fechar
		 *********************************************************************				resposta.setContent("Ok");*****/
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
//				exibirAviso(myAgent, "Agent "+getLocalName()+ ": REQUEST received from "+request.getSender().getName()+". Action is "+request.getContent());
				
				ACLMessage resposta = request.createReply();
				resposta.setPerformative(ACLMessage.AGREE);
				resposta.setContent("ok");
				
				//Antes seto no XML que o agente chave irá comandar a abertura da sua chave quando for responder ao inform de monitoramento do matlab
				agenteCBD.getChild("comando").setText("1");
				agenteCBD.getChild("estado").setText("1");   //Era pro agente chave medir isso no começo do inform para medição
				
				return resposta;
			}// Fim do protected ACLMessage prepareResponse
		} );//Fim do request responder 
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


