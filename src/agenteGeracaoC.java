/************************************************************************************************************
 * @author Fernando Américo Albuquerque Rodrigues Marçal
 * @since 20/07/2015
 * @version 1.0
 * Descrição
 * Este é o comportamento do Agente Geração Controalda (AGC). Este agente é o responsável pelo gerenciamento de um sistema de geração controlada em uma microrrede, sejam elas trifásicas 
 * ou monofásica. 
 *
 * << Lista de Abreviaturas >>
 * nomeAgente: variável que receberá o nome do agente em questão
 * Mensagem: variável que receberá a mensagem ACLMessage
 * BD: banco de dados
 * CR : conexão a rede. Diz se a urrede está conectada a rede e consequente o estado da chave do PCC. 1-conectado/fechado 0
 * 
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
import jade.proto.AchieveREResponder;
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

public class agenteGeracaoC extends Agent { // Classe "agenteGeracaoC" que por sua vez é uma subclasse
									// da classe "Agent"

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void setup()
	{
		final String nomeAgente = getLocalName(); //a variável "nomeAgente" recebe o nome local do agente 
		final Element agenteGCBD = carregaBD(nomeAgente); //Chama o método carregaBD que carrega o BD do agente "nomeAgente"
		
		//Filtro para receber somente mensagens do protocolo tipo "inform"
		final MessageTemplate filtroInformMonitoramento = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
		MessageTemplate filtroContractNet = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );   //Filtro do contract net com o APC
		
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
		addBehaviour(new TickerBehaviour(this,100){
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
//					exibirMensagem(msg);
					
					/*
					 * Parte de medição e aquisição de dados e armazenamento no XML
					 */
					String conteudo = msg.getContent();  //Pego o conteudo da mensagem
					String estadoChave = conteudo; //A mensagem contêm somente o estado da chave da geração controlada
				
//					exibirAviso(myAgent, "O estado da minha chave é: "+estadoChave);
					
					agenteGCBD.getChild("medidasAtuais").getChild("estadoChave").setText(estadoChave); /*Seta no XML o valor do estado da chave da geração controlada*/
					
					/*
					 * Parte de consulta ao XML e comando
					 * O camando será enviado como resposta ao inform da medição. São aproveitadas 2 das variáveis anteriores 
					 */
					String potencia = "0";
					estadoChave = agenteGCBD.getChild("comando").getChild("estadoChave").getText(); //Consulta no XML o valor do disjuntor a jusante do inversor
					if(estadoChave.equalsIgnoreCase("0")){
						potencia = "0"; //A geração controlada não irá gerar nada
					}else if(estadoChave.equalsIgnoreCase("1")){
						potencia = agenteGCBD.getChild("comando").getChildText("potNominal"); //consulta no XML a potência de referência que terá que ser gerada
					}
					
					ACLMessage resposta = msg.createReply();
					resposta.setContent(estadoChave+"/"+potencia); //A mensagem será no formato "estadoChave/potencia"
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
//				exibirMensagem(cfp);
				
				
				// We provide a proposal
//					System.out.println("Agent "+getLocalName()+": Proposing "+proposal);
//				double deltaP = Double.parseDouble(cfp.getContent());
//				double potenciaNominal = Double.parseDouble(agenteGCBD.getChild("potNominal").getText());
				
//				exibirAviso(myAgent, "Estou propondo: "+agenteGCBD.getChild("comando").getChild("potNominal").getText());
				
				ACLMessage propose = cfp.createReply();
				propose.setPerformative(ACLMessage.PROPOSE);
				propose.setContent(agenteGCBD.getChild("comando").getChild("potNominal").getText());
				return propose;
				
//				else {  //Assume-se que sempre tera combustível para fornecer a potencial nominal da fonte controlada
//					// We refuse to provide a proposal
////					System.out.println("Agent "+getLocalName()+": Refuse");
//					exibirAviso(myAgent, "Recusei o pedido de deltaP");
//					throw new RefuseException("O SOC da bateria está abaixo de SOC!");
//				}
			}

			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) throws FailureException {
//				System.out.println("Agent "+getLocalName()+": Proposal accepted");
//				if (performAction()) {
//					System.out.println("Agent "+getLocalName()+": Action successfully performed");
					ACLMessage inform = accept.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					
					//Antes eu dou uma atualizada no XML
					agenteGCBD.getChild("comando").getChild("estadoChave").setText("1"); /*seta o XML o disjuntor fechando para quando o agente receber o informe de monitoramento, ele ler
					 																		o esse valor de xml e respodner comandando a chave*/
//					agenteGCBD.getChild("comando").getChild("pot").setText(cfp.getContent()); /*seta no XML o valor da potência que terá que ser gerada para quando o agente receber o informe de monitoramento, ele ler
					 																		//o esse valor de xml e respodner comandando */
					
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
		} ); //Fim do comportamento contract net
		
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
