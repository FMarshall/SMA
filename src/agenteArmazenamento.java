/************************************************************************************************************
 * @author Fernando Américo Albuquerque Rodrigues Marçal
 * @since 27/04/2015
 * @version 1.0
 * Descrição
 * Este é o comportamento do Agente Armazenamento (AA). Este agente é o responsável pelo gerenciamento de um sistema de armazenamento em uma microrrede, sejam elas trifásicas 
 * ou monofásica. 
 *
 * << Lista de Abreviaturas >>
 * nomeAgente: variável que receberá o nome do agente em questão
 * Mensagem: variável que receberá a mensagem ACLMessage
 * BD: banco de dados
 * CR : conexão a rede. Diz se a urrede está conectada a rede e consequente o estado da chave do PCC. 1-conectado/fechado 0
 * SOC : estado da carga (ou do inglês, State of Charge)-
 ************************************************************************************************************/

import jade.core.AID;
import jade.core.Agent;
//import java.util.Iterator;
import jade.lang.acl.ACLMessage; //Relacionada a endereçoes
//import jade.core.AID;    //Relacionada a endereços
import jade.lang.acl.MessageTemplate; // Para uso dos filtros
//import jade.domain.FIPANames; //Para uso dos filtros
//import jade.core.behaviours.CyclicBehaviour; //Para comportamento temporal

import jade.core.behaviours.TickerBehaviour;
import jade.proto.SubscriptionResponder;
import jade.proto.ContractNetResponder;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;
//As bibliotecas abaixo foram exigidas no decorrer do SubscriptionResponder
//import jade.domain.FIPAAgentManagement.FailureException;
//import jade.domain.FIPAAgentManagement.NotUnderstoodException;
//import jade.domain.FIPAAgentManagement.RefuseException;

//Bibliotecas para lidar com arquivos XML
//import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder; //This package support classes for building JDOM documents and content using SAX parsers. 
//import org.jdom2.Attribute;


//Foram incluídas automaticamente
import java.io.File;
import java.io.IOException;
//import java.util.Iterator;
//import java.util.List; //Trantando com lista
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class agenteArmazenamento extends Agent { // Classe "agenteArmazenamento" que por sua vez é uma subclasse
									// da classe "Agent"

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void setup()
	{
		final String nomeAgente = getLocalName(); //a variável "nomeAgente" recebe o nome local do agente 
		final Element agenteAABD = carregaBD(nomeAgente); //Chama o método carregaBD que carrega o BD do agente "nomeAgente"
		
		//Filtro para receber somente mensagens do protocolo tipo "inform"
		final MessageTemplate filtroInformMonitoramento = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
//		final MessageTemplate filtroInformMonitoramento = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchSender(getAID().getLocalName()); 
			
//		tcpAA1@192.168.1.6:1099/JADE
//		AA1@192.168.1.6:1099/JADE
		MessageTemplate filtroContractNet = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );
		
//		System.out.println(".:: Agente PCC APCC1 iniciado com sucesso! ::.\n");
//		System.out.println("Todas as minhas informações: \n" +getAID());
//		System.out.println(">> Meu nome local é " + getLocalName()); // Informações completas
//		System.out.println("\n>> Meu nome global é " +getAID().getName());
//		System.out.println("Meu endereço é " +getAID().getAllAddresses());
		
//		System.out.println("\nMeus endereços são: ");
//		Iterator it = getAID().getAllAddresses();
//		while(it.hasNext()){
//			System.out.println("- "+it.next());
//		}
		/**
		 * Este comportamente temporal se divide em duas partes:
		 * Parte 1 (medição) - Aquisição de dados do matlab, no caso, de um sistema de armazenamento do tipo bateria
		 * Parte 2 (comando) - Leitura do XML para comando de chaves e modo de atuação (fonte de corrente ou tensão) 
		 */
		addBehaviour(new TickerBehaviour(this,100) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void onTick(){

				ACLMessage msg = receive(filtroInformMonitoramento); /*o intuito dessa mensagem é a monitoração do SOC e chave 
				                                      do dispositivo de armazenamento monitorado*/
				//String conteudo = mensagem.getContent();

				//if(msg_curto!=null && msg_curto.getContent()=="curto"){
				//if(msg_curto!=null && conteudo=="curto"){
				if(msg!=null){	
					exibirMensagem(msg);
					
					/**
					 * Método para fazer o teste de conexão com o matlab para entender o código em matlab
					 *******************************/
//					ACLMessage msg = msg.createReply();
////					msg.setContent("ok");
//					msg.setContent("um/dois/tres");
//					send(msg);
					//******************************
					
//					if(msg.getContent().equals("0")) {
//						System.out.println("Chave está aberta!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//					}else if(msg.getContent().equals("1")){
//						System.out.println("Chave está fechada!!!!!!!");
//					}else{
//						System.out.println("Deu pau!");
//					}
					
					/*
					 * Parte de medição e aquisição de dados e armazenamento no XML
					 */
					String conteudo = msg.getContent();  //Pego o conteudo da mensagem
					exibirAviso(myAgent, "O conteúdo da msg que recebi é: "+conteudo);
					
					String SOC = conteudo.split("/")[0]; /* A mensagem é no formato:  "SOC/estado da chave/modo atuação". Foi aplicado o método split para quebrar o "conteudo" em 
					array sendo a separação definida pelo caracter "/". // Da separação eu peguei a posição 0 da array que corresponde ao SOC do dispositivo monitorado.*/
					String estadoChave = conteudo.split("/")[1];
//					exibirAviso(myAgent, "O estado da minha chave é: "+estadoChave);
					String modoAtuacao = conteudo.split("/")[2];
					
					agenteAABD.getChild("medidasAtuais").getChild("soc").setText(SOC);	//seta o XML do agente atualizando o valor da SOC
					agenteAABD.getChild("medidasAtuais").getChild("estadoChave").setText(estadoChave); //seta o XML do agente atualizando o estado da chave
					agenteAABD.getChild("medidasAtuais").getChild("status").setText(modoAtuacao); //seta no XML se é modo corrente ou modo tensão. "0": modo corrente; "1" modo tensão
					
					/*
					 * Parte de consulta ao XML e comando
					 * O camando será enviado como resposta ao inform da medição. São aproveitadas 2 das variáveis anteriores 
					 */
					estadoChave = agenteAABD.getChild("comando").getChild("estadoChave").getText(); //Consulta no XML o valor do disjuntor a jusante do inversor
					modoAtuacao = agenteAABD.getChild("comando").getChildText("status"); //consulta no XML o modo de atuação
					String Pbat = agenteAABD.getChild("comando").getChildText("Pbat"); // valor da potência gerada quando estiver no modo fonte de correte
					
					ACLMessage resposta = msg.createReply();
					resposta.setContent(estadoChave+"/"+modoAtuacao+"/"+Pbat); //A mensagem será no formato "estadoChave/modoAtuacao/Pbat"
					send(resposta);  //enviando a mensagem de resposta do Inform ao Matlalb
					
				}// fim o if para saber se inform != null
			} // fim do onTick 
		}); //fim do comportamento temporal TickerBehaviour
		
		addBehaviour(new ContractNetResponder(this, filtroContractNet) {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
//				System.out.println("Agent "+getLocalName()+": CFP received from "+cfp.getSender().getName()+". Action is "+cfp.getContent());
				exibirMensagem(cfp);
							
//				valorSOC = Double.parseDouble(((Element) agenteAABD.getContent()).getText());
				double valorSOC = Double.parseDouble(agenteAABD.getChild("medidasAtuais").getChild("soc").getText());
								
				if (valorSOC > 80) {
					// We provide a proposal
//					System.out.println("Agent "+getLocalName()+": Proposing "+proposal);
					exibirAviso(myAgent, "Aceito a solicitação de deltaP igual a: "+cfp.getContent());
					ACLMessage propose = cfp.createReply();
					propose.setPerformative(ACLMessage.PROPOSE);
//					propose.setContent(String.valueOf(valorSOC));
					propose.setContent(cfp.getContent());
					return propose;
				}
				else {
					// We refuse to provide a proposal
//					System.out.println("Agent "+getLocalName()+": Refuse");
					exibirAviso(myAgent, "Recusei o pedido de deltaP");
					throw new RefuseException("evaluation-failed");
				}
			}

			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) throws FailureException {
//				System.out.println("Agent "+getLocalName()+": Proposal accepted");
//				if (performAction()) {
//					System.out.println("Agent "+getLocalName()+": Action successfully performed");
					ACLMessage inform = accept.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					
					//Antes eu dou uma atualizada no XML
					agenteAABD.getChild("comando").getChild("estadoChave").setText("1"); //seta o XML o disjuntor fechando
					agenteAABD.getChild("comando").getChild("status").setText("0"); //seta no XML o modo de tensão pois no matlab tem um NOT que enviará 1 para a fonte de tensão
					
					return inform;
//				}
//				else {
//					System.out.println("Agent "+getLocalName()+": Action execution failed");
//					throw new FailureException("unexpected-error");
//				}	
			}

			protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
				System.out.println("Agent "+getLocalName()+": Proposal rejected");
				
				
			}
		} );
	} // fim do public void setup
	
	/**
	 * Método para exibição de mensagens ACL
	 *  @param msg recebe uma mensagem to tipo ALCMessage
	 *  
	 */
	public void exibirMensagem(ACLMessage msg) {
		System.out.println("\n\n===============<<MENSAGEM>>==================");    	
		System.out.println("De: " + msg.getSender());
		System.out.println("Para: " + this.getName());
		System.out.println("Performativa: "+msg.getPerformative());
		System.out.println("Protocolo: "+msg.getProtocol());
		System.out.println("Conteudo: " + msg.getContent());
		
		Calendar cal = Calendar.getInstance();
    	cal.getTime();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//    	SimpleDateFormat sdf = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ssss a zzz");
    	SimpleDateFormat sdf = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ssss a");
    	System.out.println( sdf.format(cal.getTime()) );
    	
//		System.out.println(System . currentTimeMillis ());
		
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
