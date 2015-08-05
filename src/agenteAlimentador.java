//Bibliotecas JADE para multiagentes

import jade.core.Agent;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetResponder;
import jade.proto.SubscriptionInitiator;
import jade.proto.ContractNetInitiator;
import jade.proto.SubscriptionResponder;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;
//Bibliotecas JADE para comportamentos temporais
import jade.core.behaviours.Behaviour;
//import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.util.leap.ArrayList;






import java.io.File;
//Bibliotecas para lidar com arquivos XML
import java.io.IOException;
import java.lang.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
//import org.omg.CORBA.portable.UnknownException;

public class agenteAlimentador extends Agent {

	//public ACLMessage negociar;

	public void setup(){
		//Pega o nome do agente no Banco de Dados
		final String agenteAlimentador = getLocalName();
		final Element agenteALBD = carregaBD(agenteAlimentador);
	
		final MessageTemplate filtroAtuacao = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE),
		  		MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE)); 
		
		/**
		 * M�todo para receber os valores atuais de carga
		 */		
		/*
		addBehaviour(new TickerBehaviour(this,100) {
			public void onTick(){
				//ACLMessage filtroAtualizacao = receive();
				//if(filtroAtualizacao != null){
				
				ACLMessage filtroRotina = receive();
				if(filtroRotina != null){
					
					
					//exibirMensagem(agree);
					
					String agenteChave = filtroRotina.getSender().getLocalName();
					System.out.println("-<<"+agenteAlimentador+">>: recebi um valor de carga de "+agenteChave);
					String cargaDemandada = filtroRotina.getContent();
					agenteAlimentadorBD.getChild("chaves").getChild(agenteChave).setAttribute("carga",cargaDemandada); //Atualiza o seu banco de dados
				
				}
			}
		});
		*/
		/********************************************************************************************************************** 
		 *		 Parte do FIPA Subscribe participante para receeber solicitação do APC para
		 *informar o valor de potência que está sendo gerada pela geração intermitente
		 **********************************************************************************************************************/
		addBehaviour(new SubscriptionResponder(this, filtroAtuacao) {
			private static final long serialVersionUID = 1L;

			protected ACLMessage handleSubscription(ACLMessage subscription){
				exibirAviso(myAgent, "Fui informado de uma falta");
				ACLMessage resposta = subscription.createReply();
				resposta.setContent("ok");
				resposta.setPerformative(ACLMessage.AGREE); 
				
				//Atualizar  o XML para saber onde foi a falta
				String agenteChaveSobFalta = subscription.getSender().getLocalName();
				agenteALBD.getChild("chaves").getChild(agenteChaveSobFalta).setAttribute("atuacao","sim");
				
				String referencia = agenteChaveSobFalta.split("_")[1]; //Só para pegar o número do agente chave para avisar somente os ajusante
				exibirAviso(myAgent, "A referência do agente chave é "+referencia);
				
//				String referenciaDaChave = referencia.split("R")[1];
				int referenciaDaChaveAtuante =Integer.parseInt(referencia.split("R")[1]); //Só para pegar o número do agente chave para avisar somente os ajusante
				exibirAviso(myAgent, "A chave atuante é: "+referenciaDaChaveAtuante);
				
				//*********Saber se tem agente chave e quantos são
				List lista = agenteALBD.getChild("chaves").getChildren(); 
				Iterator i = lista.iterator();
				
				int cont = 0; //iniia cont com zero
				
			    while(i.hasNext()) { 
			    	Element elemento = (Element) i.next();
			    	String nome = String.valueOf(elemento.getName());
			    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
			    		cont = cont + 1; //Se houver algum agente chave, incrementa o contador
			    	}
			    }
//			    if(cont!=0){ // Se existirem agentes chave
			    if(cont>referenciaDaChaveAtuante){ //Se o número de agente chave for maior que o índice da chave atuante, é porque com certeza há chaves a jusante da chave atuante
					/**********************************************************************************
				     * Protocolo FIPA Request para solicitar que todos os ACs e APCCs abram suas chaves
				     * 
				     *********************************************************************************/
			  		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			  		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			  		msg.setContent("abra");
			  		
			  		//Cria-se uma lista para percorrer a tag CHAVES (agentes chave)
			  		List lista1 = agenteALBD.getChild("chaves").getChildren(); 
					Iterator i1 = lista1.iterator();
					
				    while(i1.hasNext()) { 
				    	Element elemento = (Element) i1.next();
				    	String nome = String.valueOf(elemento.getName());
				    	
						exibirAviso(myAgent, "Analisando se aviso ao agente chave "+nome+" que comande a abertura de seu religador.");
						
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
				    		int referenciaDaChave = Integer.parseInt(nome.split("_")[1].split("R")[1]); //Analisa-se a posição da chave visada
				    		
				    		exibirAviso(myAgent, "A referencia de "+nome+" é "+referenciaDaChave);
				    		
				    		if(referenciaDaChave>referenciaDaChaveAtuante){ //Se a chave analisada estiver localizada a jusante da chave atuante então
				    			exibirAviso(myAgent, "A referência de "+nome+" é maior que a da chave atuante que é "+referenciaDaChaveAtuante);
				    			
				    			if(elemento.getAttributeValue("atuacao").equals("nao")){ //Se o agente chave não é o que atuou então vou mandar uma mensagem pra ele
					    			
				    				exibirAviso(myAgent, "Solicitando ao agente chave "+nome+" que comande a abertura de sua chave.");
//				    				
						    		msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
						    		
						    		
				    			}// Fim do if para saber se atuou ou não
				    		}// Fim do if para saber se referenciaDaChave>referenciaDaChaveAtuante
				    	}// Fim do if para saber se há chave
				    }// Fim do while(i.hasNext())	
			  		
			  		
				    addBehaviour(new AchieveREInitiator(myAgent, msg) {
						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
						}
						protected void handleRefuse(ACLMessage refuse) {
//							System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
//							nResponders--;
						}
						protected void handleFailure(ACLMessage failure) {
							if (failure.getSender().equals(myAgent.getAMS())) {
								// FAILURE notification from the JADE runtime: the receiver
								// does not exist
								System.out.println("Responder does not exist");
							}
							else {
								System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
							}
						}
						protected void handleAllResultNotifications(Vector notifications) {
//							if (notifications.size() < nResponders) {
//								// Some responder didn't reply within the specified timeout
//								System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
//							}
						}
					}); //Fim do add
			  		
			  		
				} //Fim do if(cont>referenciaDaChaveAtuante)
			    else{ //se não tem chaves a jusante, mas ver se não tem pelo menos microrrede no mesmo trecho
			    	
			    }
//			    addBehaviour(new AchieveREInitiator(myAgent, msg) {
//					protected void handleInform(ACLMessage inform) {
//						System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
//					}
//					protected void handleRefuse(ACLMessage refuse) {
////						System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
////						nResponders--;
//					}
//					protected void handleFailure(ACLMessage failure) {
//						if (failure.getSender().equals(myAgent.getAMS())) {
//							// FAILURE notification from the JADE runtime: the receiver
//							// does not exist
//							System.out.println("Responder does not exist");
//						}
//						else {
//							System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
//						}
//					}
//					protected void handleAllResultNotifications(Vector notifications) {
////						if (notifications.size() < nResponders) {
////							// Some responder didn't reply within the specified timeout
////							System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
////						}
//					}
//				}); //Fim do add
				
			    
//			  //Cria-se uma lista para percorrer a tag CHAVES (agentes chave)
//		  		List lista2 = agenteALBD.getChild("microrredes").getChildren(); 
//				Iterator i2 = lista2.iterator();
//				
//			    while(i2.hasNext()) { 
//			    	Element elemento = (Element) i2.next();
//			    	String nome = String.valueOf(elemento.getName());
//			    	
//					exibirAviso(myAgent, "Analisando se aviso ao agente PC "+nome+" que comande a abertura de seu disjuntor.");
//					
//			    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
////			    		int referenciaDaChave = Integer.parseInt(nome.split("_")[1].split("R")[1]); //Analisa-se a posição da chave visada
//			    		
////			    		exibirAviso(myAgent, "A referencia de "+nome+" é "+referenciaDaChave);
//			    		
////			    		if(referenciaDaChave>referenciaDaChaveAtuante){ //Se a chave analisada estiver localizada a jusante da chave atuante então
////			    			exibirAviso(myAgent, "A referência de "+nome+" é maior que a da chave atuante que é "+referenciaDaChaveAtuante);
//			    			
////			    			if(elemento.getAttributeValue("atuacao").equals("nao")){ //Se o agente chave não é o que atuou então vou mandar uma mensagem pra ele
//				    			
//			    				exibirAviso(myAgent, "Solicitando ao agente "+nome+" que comande a abertura de sua chave.");
//			    				/**********************************************************************************
//			    			     * Protocolo FIPA Request para solicitar que todos os ACs e APCCs abram suas chaves
//			    			     * 
//			    			     *********************************************************************************/
//			    		  		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
//			    		  		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
//			    		  		msg.setContent("abra");
//			    		  		
//					    		msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
//					    		
//					    		addBehaviour(new AchieveREInitiator(myAgent, msg) {
//									protected void handleInform(ACLMessage inform) {
//										System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
//									}
//									protected void handleRefuse(ACLMessage refuse) {
////										System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
////										nResponders--;
//									}
//									protected void handleFailure(ACLMessage failure) {
//										if (failure.getSender().equals(myAgent.getAMS())) {
//											// FAILURE notification from the JADE runtime: the receiver
//											// does not exist
//											System.out.println("Responder does not exist");
//										}
//										else {
//											System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
//										}
//									}
//									protected void handleAllResultNotifications(Vector notifications) {
////										if (notifications.size() < nResponders) {
////											// Some responder didn't reply within the specified timeout
////											System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
////										}
//									}
//								}); //Fim do add
////			    			}// Fim do if para saber se atuou ou não
////			    		}// Fim do if para saber se referenciaDaChave>referenciaDaChaveAtuante
//			    	}// Fim do if para saber se há chave
//			    }// Fim do while(i.hasNext())
			    
			  //Cria-se uma lista para percorrer a tag MICRORREDES
//		  		List lista2 = agenteALBD.getChild("microrredes").getChildren(); 
//				Iterator i2 = lista.iterator();
//				
//			    while(i2.hasNext()) { 
//			    	Element elemento = (Element) i2.next();
//			    	String nome = String.valueOf(elemento.getName());
//			    	
//			    	exibirAviso(myAgent, "Analisando se aviso à microrrede "+nome+" que comande a abertura de seu religador.");
//					
//			    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes APC no XML, então add ele como remetente
////								System.out.println("Entrou no if!!!!!");  //Só pra testar
//			    		exibirAviso(myAgent, "Solicitando a microrrede "+nome+" que comande a abertura de sua chave.");
//			    		msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
//			    	}
//			    }		
//			 
//		  		addBehaviour(new AchieveREInitiator(myAgent, msg) {
//					protected void handleInform(ACLMessage inform) {
//						System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
//					}
//					protected void handleRefuse(ACLMessage refuse) {
////						System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
////						nResponders--;
//					}
//					protected void handleFailure(ACLMessage failure) {
//						if (failure.getSender().equals(myAgent.getAMS())) {
//							// FAILURE notification from the JADE runtime: the receiver
//							// does not exist
//							System.out.println("Responder does not exist");
//						}
//						else {
//							System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
//						}
//					}
//					protected void handleAllResultNotifications(Vector notifications) {
////						if (notifications.size() < nResponders) {
////							// Some responder didn't reply within the specified timeout
////							System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
////						}
//					}
//				}); //Fim do addBehaviour do request initiator
				
				return resposta;
			}//fim de handleSubscription
			
		});	//Fim do SubscriptionResponder
	} // fim do public void setup
		

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
