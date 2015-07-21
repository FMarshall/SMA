/************************************************************************************************************
 * @author Fernando Américo Albuquerque Rodrigues Marçal
 * @since 23/04/2015
 * @version 1.0
 * Descrição
 * Este é o comportamento do Agente Demanda (AD). Este agente é o responsável pelo gerenciamento das cargas dentro de uma microrrede, sejam elas trifásicas 
 * ou monofásica. 
 *
 * << Lista de Abreviaturas >>
 * nomeAgente: variável que receberá o nome do agente em questão
 * Mensagem: variável que receberá a mensagem ACLMessage
 * BD: banco de dados
 * CR : conexão a rede. Diz se a urrede está conectada a rede e consequente o estado da chave do PCC. 1-conectado/fechado 0-
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
import jade.domain.FIPANames;

import jade.proto.SubscriptionResponder;

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

public class agenteDemanda extends Agent { // Classe "agenteGeracao" que por sua vez é uma subclasse
									// da classe "Agent"

	double potenciaTotalCargas = 0; //potencia total de todas as cargas
	
	private static final long serialVersionUID = 1L;

	public void setup()
	{
		final String nomeAgente = getLocalName(); //a variável "nomeAgente" recebe o nome local do agente 
		final Element agenteADBD = carregaBD(nomeAgente); //Chama o método carregaBD que carrega o BD do agente "nomeAgente"
		
		//Filtro para receber somente mensagens do protocolo tipo "inform"
		final MessageTemplate filtroInformMonitoramento = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
		MessageTemplate filtroSubscribe = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE),MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE)); 
		
		
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
		 * Este comportamente temporial é somente para aquisição de dados do matlab, no caso, de um sistema de geração
		 * intermitente. 
		 */
		addBehaviour(new TickerBehaviour(this,100) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void onTick(){

				
				
				ACLMessage msg = receive(filtroInformMonitoramento); /*o intuito dessa mensagem é a monitoração da potência e chave 
				                                      da geração intermitente monitorada*/
				//String conteudo = mensagem.getContent();

				//if(msg_curto!=null && msg_curto.getContent()=="curto"){
				//if(msg_curto!=null && conteudo=="curto"){
				if(msg!=null){	
					exibirMensagem(msg);
					
//					if(msg.getContent().equals("0")) {
//						System.out.println("Chave está aberta!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//					}else if(msg.getContent().equals("1")){
//						System.out.println("Chave está fechada!!!!!!!");
//					}else{
//						System.out.println("Deu pau!");
//					}
					
					String conteudo = msg.getContent();  //Pego o conteudo da mensagem
					
					String valorCarga = conteudo.split("/")[0]; // Valor da carga demandada
					String estadoChave = conteudo.split("/")[1]; // Estado da chave
					String refCarga = conteudo.split("/") [2]; /*A mensagem é no formato:  "referencia da carga/potencia demandada". Foi aplicado o método split para quebrar o "conteudo" em 
					array sendo a separação definida pelo caracter "/". Da separação eu peguei a posição 0 da array que corresponde a referência da carga monitorada, visto que pode ter várias cargas.*/
//					System.out.println("A referencia da carga é: "+refCarga); //Só pra testar se tava dando certo
					
					agenteADBD.getChild("cargas").getChild(refCarga).setAttribute("demanda",valorCarga);	//seta o XML do agente atualizando o valor da demanda
					
					/*Seta no XML o valor da potência gerada pelo sistema de geração intermitente*/
					/* Essa parte é opcional. Creio que não seja necessário responder ao matlab que deu certo.
					 * ACLMessage resposta = msg.createReply();
					resposta.setPerformative(ACLMessage.AGREE);
					resposta.setContent("Recebido!");
					myAgent.send(resposta);*/
					
//					agenteApcBD.getChild("estado").setText("aberta");
					
					
				}// fim o if para saber se inform != null
			} // fim do onTick 
		}); //fim do comportamento temporal TickerBehaviour
		
	
		/********************************************************************************************************************** 
		 *		 Parte do FIPA Subscribe participante para receeber solicitação do APC para
		 *informar o valor de potência que está sendo gerada pela geração intermitente
		 **********************************************************************************************************************/
		
		addBehaviour(new SubscriptionResponder(this, filtroSubscribe) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			protected ACLMessage handleSubscription(ACLMessage subscription){
				ACLMessage resposta = subscription.createReply();
//				String Potencia = agenteADBD.getChild("cargas").getChildText("potencia");
				/*Tenho que acessar todas os valores de todas as cargas. Faço um while pra acessar os elementos do XML e ir 
				somando as potências demandadas*/
				
				List lista = agenteADBD.getChild("cargas").getChildren(); 
//				System.out.println("A referência das cargas são: "+lista);  //Sò pra testar se deu certo
				Iterator i = lista.iterator();
				
			    while(i.hasNext()) {
			    	Element elemento = (Element) i.next();
			    	String nome = String.valueOf(elemento.getName());
//					System.out.println("O nome é: "+nome);  //Só pra testar 
//			    	exibirAviso(myAgent, "Solicitando valor de potencia demandada a: "+nome);
					double valorCarga = 0;
			    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes geradores no XML, então add ele como remetente
//								System.out.println("Entrou no if!!!!!");  //Só pra testar
			    		
			    		valorCarga = Double.parseDouble(elemento.getAttributeValue("demanda"));
			    		exibirAviso(myAgent, "O valor da carga de " +nome+" é " +valorCarga);
			    		potenciaTotalCargas = potenciaTotalCargas + valorCarga;
			    		valorCarga = 0;
			    	}
			    }
				
				resposta.setContent(String.valueOf(potenciaTotalCargas));
				resposta.setPerformative(ACLMessage.AGREE);
				potenciaTotalCargas = 0; //Como já setei o conteúdo da resposta, posso zerar a variável.
				
				return resposta;
				
			}//fim de handleSubscription
			
		});	//Fim do SubscriptionResponder
		
		
		
		
		
		
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
